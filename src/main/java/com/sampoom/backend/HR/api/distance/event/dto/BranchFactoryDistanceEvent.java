package com.sampoom.backend.HR.api.distance.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchFactoryDistanceEvent {

    private String eventId;
    private String eventType;
    private Long version;
    private String occurredAt;
    private Payload payload;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private Long distanceId;
        private Long branchId;
        private Long factoryId;
        private Double distanceKm;
        private String branchName;
        private String factoryName;
    }
}
