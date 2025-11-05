package com.sampoom.backend.HR.common.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.backend.HR.api.branch.event.dto.BranchEvent;
import com.sampoom.backend.HR.api.distance.event.dto.BranchAgencyDistanceEvent;
import com.sampoom.backend.HR.api.distance.event.dto.BranchFactoryDistanceEvent;
import com.sampoom.backend.HR.api.vendor.event.dto.VendorEvent;
import com.sampoom.backend.HR.common.outbox.entity.Outbox;
import com.sampoom.backend.HR.common.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventProcessor {

    // Kafka 토픽 이름 정의
    private static final String TOPIC_BRANCH = "branch-events";
    private static final String TOPIC_BRANCH_DISTANCE = "branch-distance-events";
    private static final String TOPIC_VENDOR = "vendor-events";
    private static final String TOPIC_BRANCH_FACTORY = "factory-branch-events";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 개별 Outbox 이벤트 발행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAndPublishEvent(Outbox outbox) {
        String topicName;
        Object eventToSend; // Kafka로 보낼 최종 이벤트 DTO
        String eventKey = outbox.getAggregateId().toString();

        // 최대 재시도 횟수 체크
        if (outbox.getRetryCount() >= 10) {
            log.warn("[OutboxEvent] 최대 재시도 횟수 초과로 발행 중단: eventId={}, retryCount={}",
                    outbox.getEventId(), outbox.getRetryCount());
            return;
        }

        try {
            // AggregateType에 따라 DTO 역질렬화 및 토픽/이벤트 구성
            switch (outbox.getAggregateType()) {
                case "BRANCH":
                case "FACTORY":
                    BranchEvent.Payload branchPayload =
                            objectMapper.readValue(outbox.getPayload(), BranchEvent.Payload.class);
                    eventToSend = BranchEvent.builder()
                            .eventId(outbox.getEventId())
                            .eventType(outbox.getEventType())
                            .version(outbox.getVersion())
                            .occurredAt(outbox.getOccurredAt().toString())
                            .payload(branchPayload)
                            .build();

                    // FACTORY면 factory-events로 보냄, 아니면 branch-events로
                    topicName = outbox.getAggregateType().equals("FACTORY")
                            ? TOPIC_BRANCH_FACTORY : TOPIC_BRANCH;
                    break;

                // 창고-공장 거리 이벤트
                case "BRANCH_FACTORY_DISTANCE":
                    BranchFactoryDistanceEvent.Payload factoryDistancePayload =
                            objectMapper.readValue(outbox.getPayload(), BranchFactoryDistanceEvent.Payload.class);
                    eventToSend = BranchFactoryDistanceEvent.builder()
                            .eventId(outbox.getEventId())
                            .eventType(outbox.getEventType())
                            .version(outbox.getVersion())
                            .occurredAt(outbox.getOccurredAt().toString())
                            .payload(factoryDistancePayload)
                            .build();
                    topicName = "branch-factory-distance-events";
                    break;

                // 창고-대리점 거리 이벤트
                case "BRANCH_AGENCY_DISTANCE":
                    BranchAgencyDistanceEvent.Payload agencyDistancePayload =
                            objectMapper.readValue(outbox.getPayload(), BranchAgencyDistanceEvent.Payload.class);
                    eventToSend = BranchAgencyDistanceEvent.builder()
                            .eventId(outbox.getEventId())
                            .eventType(outbox.getEventType())
                            .version(outbox.getVersion())
                            .occurredAt(outbox.getOccurredAt().toString())
                            .payload(agencyDistancePayload)
                            .build();
                    topicName = TOPIC_BRANCH_DISTANCE;
                    break;

                // 기존 DISTANCE 타입 (하위 호환성)
                case "DISTANCE":
                    // BranchAgencyDistanceEvent와 BranchFactoryDistanceEvent 구분
                    if (outbox.getEventType().equals("BranchFactoryDistanceCalculated")) {
                        BranchFactoryDistanceEvent.Payload legacyFactoryDistancePayload =
                                objectMapper.readValue(outbox.getPayload(), BranchFactoryDistanceEvent.Payload.class);
                        eventToSend = BranchFactoryDistanceEvent.builder()
                                .eventId(outbox.getEventId())
                                .eventType(outbox.getEventType())
                                .version(outbox.getVersion())
                                .occurredAt(outbox.getOccurredAt().toString())
                                .payload(legacyFactoryDistancePayload)
                                .build();
                        topicName = "branch-factory-distance-events";
                    } else {
                        // 기존 BranchAgencyDistanceEvent 처리
                        BranchAgencyDistanceEvent.Payload legacyDistancePayload =
                                objectMapper.readValue(outbox.getPayload(), BranchAgencyDistanceEvent.Payload.class);
                        eventToSend = BranchAgencyDistanceEvent.builder()
                                .eventId(outbox.getEventId())
                                .eventType(outbox.getEventType())
                                .version(outbox.getVersion())
                                .occurredAt(outbox.getOccurredAt().toString())
                                .payload(legacyDistancePayload)
                                .build();
                        topicName = TOPIC_BRANCH_DISTANCE;
                    }
                    break;

                case "VENDOR":
                    VendorEvent.Payload vendorPayload =
                            objectMapper.readValue(outbox.getPayload(), VendorEvent.Payload.class);
                    eventToSend = VendorEvent.builder()
                            .eventId(outbox.getEventId())
                            .eventType(outbox.getEventType())
                            .version(outbox.getVersion())
                            .occurredAt(outbox.getOccurredAt().toString())
                            .payload(vendorPayload)
                            .build();
                    topicName = TOPIC_VENDOR;
                    break;

                default:
                    throw new IllegalStateException("알 수 없는 AggregateType: " + outbox.getAggregateType());
            }

            // Kafka 발행 (동기식 처리)
            kafkaTemplate.send(topicName, eventKey, eventToSend).get(30, TimeUnit.SECONDS);

            // 발행 성공 시 처리 (같은 트랜잭션)
            outbox.markPublished();
            outboxRepository.save(outbox);
            log.info("[OutboxEvent] Kafka 발행 성공: {} ({})", outbox.getEventType(), outbox.getEventId());

        } catch (Exception e) {
            // Kafka 발행 실패 또는 DB 업데이트 실패 시
            outbox.markFailed();
            outboxRepository.save(outbox);

            // 재시도 횟수에 따른 로그 레벨 조정
            if (outbox.getRetryCount() >= 10) {
                log.error("[OutboxEvent] 최종 발행 실패 (재시도 한계 도달): eventId={}, retryCount={}, reason={}",
                        outbox.getEventId(), outbox.getRetryCount(), e.getMessage());
            } else {
                log.warn("[OutboxEvent] 발행 실패 (재시도 예정): eventId={}, retryCount={}, reason={}",
                        outbox.getEventId(), outbox.getRetryCount(), e.getMessage());
            }
        }
    }
}
