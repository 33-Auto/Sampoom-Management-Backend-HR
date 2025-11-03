package com.sampoom.backend.HR.common.outbox.entity;

import com.sampoom.backend.HR.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Outbox extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;   // 예: "Part"
    private Long aggregateId;       // 예: partId
    private String eventType;       // 예: "PartCreatedEvent"

    @Column(columnDefinition = "TEXT")
    private String payload;         // JSON 데이터

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;    // READY / PUBLISHED / FAILED

    private int retryCount;

    private LocalDateTime publishedAt;

    @Column(nullable = false, updatable = false, unique = true)
    private String eventId;         // 이벤트 고유 ID (UUID)

    @Column(nullable = false)
    private Long version;           // Part 엔티티 버전

    @Column(nullable = false)
    private OffsetDateTime occurredAt; // 이벤트 발생 시간 (ISO-8601)

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
        this.retryCount += 1;
    }

    @PrePersist
    public void prePersist() {
        if (this.eventId == null) {
            this.eventId = UUID.randomUUID().toString();
        }
        if (this.occurredAt == null) {
            this.occurredAt = OffsetDateTime.now();
        }
        if (this.status == null) {
            this.status = OutboxStatus.READY;
        }
        if (this.version == null) {
            this.version = 1L; // 혹은 part.getVersion()과 연동 가능
        }
    }
}
