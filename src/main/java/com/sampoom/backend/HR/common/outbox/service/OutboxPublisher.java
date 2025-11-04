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

    // 최대 재시도 횟수
    private static final int MAX_RETRY_COUNT = 10;

    /**
     * READY와 FAILED 상태 Outbox를 5초마다 Kafka로 발행 (최대 10번 재시도)
     */
    @Scheduled(fixedDelay = 5000)
    public void publishReadyAndFailedEvents() {
        // READY 상태와 재시도 횟수가 10회 미만인 FAILED 상태 이벤트 조회
        List<Outbox> events = outboxRepository.findTop10ByStatusReadyOrFailedWithRetryLimitOrderByCreatedAtAsc(MAX_RETRY_COUNT);

        if (events.isEmpty()) return;

        log.info("[OutboxPublisher] 발행할 이벤트 {}개 발견 (READY + FAILED 재시도 대상)", events.size());

        for (Outbox outbox : events) {
            try {
                // 재시도 로그 출력
                if (outbox.getStatus() == OutboxStatus.FAILED) {
                    log.info("[OutboxPublisher] FAILED 이벤트 재시도: eventId={}, retryCount={}/{}",
                            outbox.getEventId(), outbox.getRetryCount(), MAX_RETRY_COUNT);
                }

                // 개별 트랜잭션으로 처리하기 위해 public 메서드 호출
                outboxEventProcessor.processAndPublishEvent(outbox);
            } catch (Exception e) {
                log.error("[OutboxPublisher] 이벤트 처리 중 심각한 오류 발생 (트랜잭션 롤백됨): eventId={}, reason={}",
                        outbox.getEventId(), e.getMessage());
            }
        }
    }
}