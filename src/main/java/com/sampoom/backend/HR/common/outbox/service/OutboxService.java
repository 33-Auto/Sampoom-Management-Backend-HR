package com.sampoom.backend.HR.common.outbox.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.backend.HR.common.exception.BadRequestException;
import com.sampoom.backend.HR.common.outbox.entity.Outbox;
import com.sampoom.backend.HR.common.outbox.entity.OutboxStatus;
import com.sampoom.backend.HR.common.outbox.repository.OutboxRepository;
import com.sampoom.backend.HR.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Outbox 테이블에 이벤트 저장 (트랜잭션 내)
     */
    @Transactional
    public void saveEvent(String aggregateType, Long aggregateId, String eventType, Long version, Object payloadData) {

        log.debug("Saving event to outbox: type={}, aggregateId={}", eventType, aggregateId);

        try {
            // 1. payload DTO를 JSON 문자열로 직렬화
            String payloadJson = objectMapper.writeValueAsString(payloadData);

            // 2. Outbox 엔티티 생성
            Outbox outbox = Outbox.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .version(version)
                    .status(OutboxStatus.READY)
                    .retryCount(0)
                    .build();
            // (eventId, occurredAt은 Outbox 엔티티의 @PrePersist가 자동으로 처리)

            // 3. Outbox 테이블에 저장
            outboxRepository.save(outbox);

        } catch (Exception e) {
            log.error("Outbox 이벤트 저장 실패 (메인 트랜잭션 롤백됨): {}", e.getMessage(), e);
            throw new BadRequestException(ErrorStatus.INTERNAL_SERVER_ERROR);
        }
    }
}