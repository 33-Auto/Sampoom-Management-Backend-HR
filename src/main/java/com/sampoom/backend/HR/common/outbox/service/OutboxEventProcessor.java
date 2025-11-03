package com.sampoom.backend.HR.common.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.backend.HR.api.branch.event.dto.BranchEvent;
import com.sampoom.backend.HR.api.distance.event.dto.BranchAgencyDistanceEvent;
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

        try {
            // AggregateType에 따라 DTO 역질렬화 및 토픽/이벤트 구성
            switch (outbox.getAggregateType()) {
                case "BRANCH":
                    BranchEvent.Payload branchPayload =
                            objectMapper.readValue(outbox.getPayload(), BranchEvent.Payload.class);
                    eventToSend = BranchEvent.builder()
                            .eventId(outbox.getEventId())
                            .eventType(outbox.getEventType())
                            .version(outbox.getVersion())
                            .occurredAt(outbox.getOccurredAt().toString())
                            .payload(branchPayload)
                            .build();
                    topicName = TOPIC_BRANCH;
                    break;

                // 창고-거래처 거리 이벤트 추가
                case "DISTANCE":
                    BranchAgencyDistanceEvent.Payload distancePayload =
                            objectMapper.readValue(outbox.getPayload(), BranchAgencyDistanceEvent.Payload.class);
                    eventToSend = BranchAgencyDistanceEvent.builder()
                            .eventId(outbox.getEventId())
                            .eventType(outbox.getEventType())
                            .version(outbox.getVersion())
                            .occurredAt(outbox.getOccurredAt().toString())
                            .payload(distancePayload)
                            .build();
                    topicName = TOPIC_BRANCH_DISTANCE;
                    break;

                default:
                    throw new IllegalStateException("알 수 없는 AggregateType: " + outbox.getAggregateType());
            }

            // Kafka 발행 (동기식 처리)
            // Kafka가 "잘 받았다"고 응답할 때까지 10초간 기다립니다.
            kafkaTemplate.send(topicName, eventKey, eventToSend).get(10, TimeUnit.SECONDS);

            // 발행 성공 시 처리 (같은 트랜잭션)
            outbox.markPublished();
            outboxRepository.save(outbox);
            log.info("[OutboxEvent] Kafka 발행 성공: {} ({})", outbox.getEventType(), outbox.getEventId());

        } catch (Exception e) {
            // Kafka 발행 실패 또는 DB 업데이트 실패 시
            outbox.markFailed();
            outboxRepository.save(outbox);
            log.error("[OutboxEvent] 발행 실패 (FAILED 처리): eventId={}, reason={}", outbox.getEventId(), e.getMessage());
        }
    }
}
