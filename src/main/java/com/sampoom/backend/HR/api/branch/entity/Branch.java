package com.sampoom.backend.HR.api.branch.entity;

import com.sampoom.backend.HR.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "branch")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Branch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String branchCode;   // 지점 코드 (예: WH-001, FC-001)

    @Column(nullable = false, length = 100)
    private String name;         // 지점 이름

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BranchType type;     // WAREHOUSE / FACTORY

    @Column(length = 255)
    private String address;      // 주소

    private Double latitude;     // 위도 (nullable)
    private Double longitude;    // 경도 (nullable)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BranchStatus status = BranchStatus.ACTIVE;

    /** 지점 정보 수정 */
    public void updateInfo(String name, String address, BranchStatus status) {
        if (name != null) this.name = name;
        if (address != null) this.address = address;
        if (status != null) this.status = status;
    }

    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    /** 지점 비활성화 */
    public void deactivate() {
        this.status = BranchStatus.INACTIVE;
    }
}
