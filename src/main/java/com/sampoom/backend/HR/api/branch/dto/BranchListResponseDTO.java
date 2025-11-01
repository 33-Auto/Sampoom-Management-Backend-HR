package com.sampoom.backend.HR.api.branch.dto;

import com.sampoom.backend.HR.api.branch.entity.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchListResponseDTO {

    private Long id;
    private String branchCode;
    private String name;
    private BranchType type;
    private String address;
    private BranchStatus status;

    public static BranchListResponseDTO from(Branch b) {
        return BranchListResponseDTO.builder()
                .id(b.getId())
                .branchCode(b.getBranchCode())
                .name(b.getName())
                .type(b.getType())
                .address(b.getAddress())
                .status(b.getStatus())
                .build();
    }
}
