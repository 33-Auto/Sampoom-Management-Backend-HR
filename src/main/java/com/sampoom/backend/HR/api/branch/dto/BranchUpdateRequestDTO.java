package com.sampoom.backend.HR.api.branch.dto;

import com.sampoom.backend.HR.api.branch.entity.BranchStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BranchUpdateRequestDTO {

    private String name;
    private String address;
    private BranchStatus status;
}