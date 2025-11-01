package com.sampoom.backend.HR.api.vendor.dto;

import com.sampoom.backend.HR.api.vendor.entity.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorResponseDTO {

    private Long id;
    private String vendorCode;
    private String name;
    private VendorType type;
    private String businessNumber;
    private String ceoName;

    private VendorStatus status;
//    private Long managerEmployeeId;    // 담당 직원 ID

    public static VendorResponseDTO from(Vendor v) {
        return VendorResponseDTO.builder()
                .id(v.getId())
                .vendorCode(v.getVendorCode())
                .name(v.getName())
                .type(v.getType())
                .businessNumber(v.getBusinessNumber())
                .ceoName(v.getCeoName())
                .status(v.getStatus())
//                .managerEmployeeId(v.getManagerEmployeeId())
                .build();
    }
}
