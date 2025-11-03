package com.sampoom.backend.HR.common.outbox.repository;

import com.sampoom.backend.HR.common.outbox.entity.Outbox;
import com.sampoom.backend.HR.common.outbox.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    List<Outbox> findTop10ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    boolean existsByAggregateIdAndStatus(Long aggregateId, OutboxStatus status);

    // ⭐️ [추가] 부트스트랩에서 중복 저장을 방지하기 위해 추가
    boolean existsByAggregateIdAndAggregateType(Long aggregateId, String aggregateType);
}
