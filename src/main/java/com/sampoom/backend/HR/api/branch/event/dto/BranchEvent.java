package com.sampoom.backend.HR.api.branch.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchEvent {

    private String eventId;       // UUID (고유 이벤트 ID)
    private String eventType;     // "BranchCreated", "BranchUpdated", "BranchDeleted"
    private Long version;         // Branch 엔티티의 @Version 값
    private String occurredAt;    // ISO-8601 시각 문자열
    private Payload payload;      // 실제 데이터 (지점 정보)

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Payload {
        private Long branchId;
        private String branchCode;
        private String branchName;
        private String address;
        private Double latitude;
        private Double longitude;
        private String status;       // ACTIVE / INACTIVE 등
        private String type;
        private Boolean deleted;     // 논리삭제 여부
    }
}
