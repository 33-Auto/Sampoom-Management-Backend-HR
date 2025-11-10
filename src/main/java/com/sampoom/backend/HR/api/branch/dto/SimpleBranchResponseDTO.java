package com.sampoom.backend.HR.api.branch.dto;

import com.sampoom.backend.HR.api.branch.entity.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleBranchResponseDTO {

    private Long id;
    private String branchCode;
    private String name;
    private BranchStatus status;

    public static SimpleBranchResponseDTO from(Branch branch) {
        return SimpleBranchResponseDTO.builder()
                .id(branch.getId())
                .branchCode(branch.getBranchCode())
                .name(branch.getName())
                .status(branch.getStatus())
                .build();
    }
}
