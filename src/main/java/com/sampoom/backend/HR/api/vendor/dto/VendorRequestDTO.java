package com.sampoom.backend.HR.api.vendor.dto;

import com.sampoom.backend.HR.api.vendor.entity.Vendor;
import com.sampoom.backend.HR.api.vendor.entity.VendorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorRequestDTO {

    private String name;
    private String businessNumber;
    private String ceoName;
    private String address;
    private VendorStatus status;
//    private Long managerEmployeeId;    // 담당 직원 ID

    public Vendor toEntity(String generatedCode) {
        return Vendor.builder()
                .vendorCode(generatedCode)
                .name(name)
                .businessNumber(businessNumber)
                .ceoName(ceoName)
                .address(address)
                .status(status != null ? status : VendorStatus.ACTIVE)
//                .managerEmployeeId(managerEmployeeId)
                .build();
    }
}
