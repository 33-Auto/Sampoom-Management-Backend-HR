package com.sampoom.backend.HR.api.distance.entity;

import com.sampoom.backend.HR.api.branch.entity.Branch;
import com.sampoom.backend.HR.api.vendor.entity.Vendor;
import com.sampoom.backend.HR.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "branch_vendor_distance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "vendor_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BranchVendorDistance extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    private Double distanceKm;

    public void updateDistance(Double newDistance) {
        this.distanceKm = newDistance;
    }
}
