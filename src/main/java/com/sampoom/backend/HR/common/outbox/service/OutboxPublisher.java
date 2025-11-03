package com.sampoom.backend.HR.common.outbox.service;

import com.sampoom.backend.HR.common.outbox.entity.Outbox;
import com.sampoom.backend.HR.common.outbox.entity.OutboxStatus;
import com.sampoom.backend.HR.common.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final OutboxEventProcessor outboxEventProcessor;

    /**
     * READY 상태 Outbox 5초마다 Kafka로 발행
     */
    @Scheduled(fixedDelay = 5000)
    public void publishReadyEvents() {
        List<Outbox> events = outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(OutboxStatus.READY);

        if (events.isEmpty()) return;

        log.info("[PartOutboxPublisher] 발행할 이벤트 {}개 발견", events.size());

        for (Outbox outbox : events) {
            try {
                // 개별 트랜잭션으로 처리하기 위해 public 메서드 호출
                outboxEventProcessor.processAndPublishEvent(outbox);
            } catch (Exception e) {
                log.error("[PartOutboxPublisher] 이벤트 처리 중 심각한 오류 발생 (트랜잭션 롤백됨): eventId={}, reason={}",
                        outbox.getEventId(), e.getMessage());
            }
        }
    }
}