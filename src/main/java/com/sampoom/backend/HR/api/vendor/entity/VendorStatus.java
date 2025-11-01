package com.sampoom.backend.HR.api.vendor.entity;

import lombok.Getter;

@Getter
public enum VendorStatus {
    ACTIVE("활성"),
    INACTIVE("비활성");

    private final String description;

    VendorStatus(String description) {
        this.description = description;
    }
}
