package com.sampoom.backend.HR.api.vendor.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorEvent {

    private String eventId;       // UUID
    private String eventType;     // VendorCreated, VendorUpdated, VendorDeleted
    private Long version;         // Optimistic Lock 버전
    private String occurredAt;    // 발생 시각 (ISO 형식)
    private Payload payload;      // 실제 데이터

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Payload {
        private Long vendorId;
        private String vendorCode;
        private String vendorName;
        private String address;
        private Double latitude;
        private Double longitude;
        private String businessNumber;
        private String ceoName;
        private String status;
        private boolean deleted;
    }
}
