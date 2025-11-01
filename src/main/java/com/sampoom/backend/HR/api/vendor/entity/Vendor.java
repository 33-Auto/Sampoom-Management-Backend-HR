package com.sampoom.backend.HR.api.vendor.entity;

import com.sampoom.backend.HR.common.entitiy.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vendor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Vendor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String vendorCode;

    @Column(nullable = false, length = 100)
    private String name;

    private String businessNumber;

    private String ceoName;  // 대표자

    private String address;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    private VendorStatus status = VendorStatus.ACTIVE;

//    private Long managerEmployeeId;  // 담당 직원 ID

    // 거래처 비활성화
    public void deactivate() {
        this.status = VendorStatus.INACTIVE;
    }

    // 개별 필드 변경 메서드
    public void changeInfo(String name, String businessNumber, String ceoName, String address, VendorStatus status) {
        if (name != null) this.name = name;
        if (businessNumber != null) this.businessNumber = businessNumber;
        if (ceoName != null) this.ceoName = ceoName;
        if (address != null) this.address = address;
        if (status != null) this.status = status;
    }
}
