package com.sampoom.backend.HR.api.distance.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchAgencyDistanceEvent {

    private String eventId;       // UUID (고유 이벤트 ID)
    private String eventType;     // "DistanceCalculated", "DistanceUpdated", "DistanceDeleted"
    private Long version;         // 거리 테이블의 버전
    private String occurredAt;    // ISO-8601 시각 문자열
    private Payload payload;      // 실제 데이터 (거리 정보)

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Payload {
        private Long distanceId;    // 거리 테이블 PK
        private Long branchId;
        private Long agencyId;
        private Double distanceKm;  // 거리(km)
        private String travelTime;  // (선택) 예: "15m"
        private Boolean deleted;
    }
}
