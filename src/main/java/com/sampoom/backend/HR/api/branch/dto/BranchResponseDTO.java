package com.sampoom.backend.HR.api.branch.dto;

import com.sampoom.backend.HR.api.branch.entity.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponseDTO {

    private Long id;
    private String branchCode;
    private String name;
    private BranchType type;
    private String address;
    private BranchStatus status;

    public static BranchResponseDTO from(Branch b) {
        return BranchResponseDTO.builder()
                .id(b.getId())
                .branchCode(b.getBranchCode())
                .name(b.getName())
                .type(b.getType())
                .address(b.getAddress())
                .status(b.getStatus())
                .build();
    }
}