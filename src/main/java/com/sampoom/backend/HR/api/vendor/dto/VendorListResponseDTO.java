package com.sampoom.backend.HR.api.vendor.dto;

import com.sampoom.backend.HR.api.vendor.entity.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorListResponseDTO {

    private Long id;               // 거래처 ID
    private String vendorCode;     // 거래처 코드
    private String name;           // 거래처명
    private VendorType type;       // 거래처 유형 (CUSTOMER / SUPPLIER / OUTSOURCE)
    private String businessNumber;
    private String ceoName;        // 대표자명
    private VendorStatus status;   // 활성화 상태 (ACTIVE / INACTIVE)

    public static VendorListResponseDTO from(Vendor v) {
        return VendorListResponseDTO.builder()
                .id(v.getId())
                .vendorCode(v.getVendorCode())
                .name(v.getName())
                .type(v.getType())
                .businessNumber(v.getBusinessNumber())
                .ceoName(v.getCeoName())
                .status(v.getStatus())
                .build();
    }
}
