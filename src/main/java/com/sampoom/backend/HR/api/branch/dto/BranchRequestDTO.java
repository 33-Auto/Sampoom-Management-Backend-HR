package com.sampoom.backend.HR.api.branch.dto;

import com.sampoom.backend.HR.api.branch.entity.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchRequestDTO {

    private String name;
    private BranchType type;       // 창고 or 공장
    private String address;
    private BranchStatus status;

    public Branch toEntity(String generatedCode) {
        return Branch.builder()
                .branchCode(generatedCode)
                .name(name)
                .type(type)
                .address(address)
                .status(status != null ? status : BranchStatus.ACTIVE)
                .build();
    }

}
