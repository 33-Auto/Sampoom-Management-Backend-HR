package com.sampoom.backend.HR.common.outbox.repository;

import com.sampoom.backend.HR.common.outbox.entity.Outbox;
import com.sampoom.backend.HR.common.outbox.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    List<Outbox> findTop10ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    boolean existsByAggregateIdAndStatus(Long aggregateId, OutboxStatus status);

    // ⭐️ [추가] 부트스트랩에서 중복 저장을 방지하기 위해 추가
    boolean existsByAggregateIdAndAggregateType(Long aggregateId, String aggregateType);

    // ⭐️ [추가] READY와 FAILED 상태를 모두 조회하되, 재시도 횟수가 maxRetryCount 미만인 것만 조회
    @Query("SELECT o FROM Outbox o WHERE (o.status = 'READY' OR (o.status = 'FAILED' AND o.retryCount < :maxRetryCount)) ORDER BY o.createdAt ASC")
    List<Outbox> findTop10ByStatusReadyOrFailedWithRetryLimitOrderByCreatedAtAsc(@Param("maxRetryCount") int maxRetryCount);
}
