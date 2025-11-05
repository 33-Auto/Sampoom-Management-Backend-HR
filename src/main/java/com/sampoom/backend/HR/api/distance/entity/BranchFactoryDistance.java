package com.sampoom.backend.HR.api.distance.entity;

import com.sampoom.backend.HR.api.branch.entity.Branch;
import com.sampoom.backend.HR.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "branch_factory_distance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "factory_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BranchFactoryDistance extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id", nullable = false)
    private Branch factory; // FACTORY 타입인 Branch 엔티티

    private Double distanceKm;

    @Version
    private Long version; // 낙관적 락 & 이벤트 버전 관리

    public void updateDistance(Double newDistance) {
        this.distanceKm = newDistance;
    }
}
