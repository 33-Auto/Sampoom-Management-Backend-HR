package com.sampoom.backend.HR.api.vendor.dto;

import com.sampoom.backend.HR.api.vendor.entity.VendorStatus;
import com.sampoom.backend.HR.api.vendor.entity.VendorType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VendorUpdateRequestDTO {

    private String name;             // 거래처명
    private String businessNumber;   // 사업자번호
    private String ceoName;          // 대표자명
    private VendorStatus status;     // 거래처 상태 (ACTIVE / INACTIVE)
}
