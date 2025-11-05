package com.sampoom.backend.HR.api.vendor.entity;

import com.sampoom.backend.HR.common.entity.BaseTimeEntity;
import com.sampoom.backend.HR.common.util.DistanceUtil;
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

    @Version
    private Long version; // 낙관적 락 & 이벤트 버전 관리

//    private Long managerEmployeeId;  // 담당 직원 ID

    public void changeInfo(String name, String businessNumber, String ceoName, String address, VendorStatus status) {
        if (name != null) this.name = name;
        if (businessNumber != null) this.businessNumber = businessNumber;
        if (ceoName != null) this.ceoName = ceoName;
        if (address != null) this.address = address;
        if (status != null) this.status = status;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude != null ? DistanceUtil.roundToTwoDecimalPlaces(latitude) : null;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude != null ? DistanceUtil.roundToTwoDecimalPlaces(longitude) : null;
    }


    // 거래처 비활성화
    public void deactivate() {
        this.status = VendorStatus.INACTIVE;
    }
}
