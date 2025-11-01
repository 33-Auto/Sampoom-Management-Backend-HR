package com.sampoom.backend.HR.api.vendor.entity;

import lombok.Getter;

@Getter
public enum VendorType {
    CUSTOMER("고객사"),
    SUPPLIER("공급업체"),
    OUTSOURCE("외주업체");

    private final String description;

    VendorType(String description) {
        this.description = description;
    }
}
