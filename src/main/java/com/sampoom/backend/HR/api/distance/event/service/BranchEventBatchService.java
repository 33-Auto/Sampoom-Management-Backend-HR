package com.sampoom.backend.HR.api.distance.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.backend.HR.api.branch.entity.Branch;
import com.sampoom.backend.HR.api.branch.entity.BranchType;
import com.sampoom.backend.HR.api.branch.event.dto.BranchEvent;
import com.sampoom.backend.HR.api.branch.repository.BranchRepository;
import com.sampoom.backend.HR.api.distance.entity.BranchVendorDistance;
import com.sampoom.backend.HR.api.distance.event.dto.BranchAgencyDistanceEvent;
import com.sampoom.backend.HR.api.distance.repository.DistanceRepository;
import com.sampoom.backend.HR.common.outbox.entity.Outbox;
import com.sampoom.backend.HR.common.outbox.entity.OutboxStatus;
import com.sampoom.backend.HR.common.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchEventBatchService {

    private final BranchRepository branchRepository;
    private final DistanceRepository distanceRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * 모든 창고(WAREHOUSE) 데이터를 Outbox에 등록
     */
    public void publishAllBranchEvents() {
        List<Branch> branches = branchRepository.findAll();

        for (Branch branch : branches) {
            try {
                // 공장은 스킵
                if (branch.getType() != BranchType.WAREHOUSE) continue;

                BranchEvent.Payload payload = BranchEvent.Payload.builder()
                        .branchId(branch.getId())
                        .branchCode(branch.getBranchCode())
                        .branchName(branch.getName())
                        .address(branch.getAddress())
                        .latitude(branch.getLatitude())
                        .longitude(branch.getLongitude())
                        .status(branch.getStatus().name())
                        .deleted(false)
                        .build();

                Outbox outbox = Outbox.builder()
                        .aggregateType("BRANCH")
                        .aggregateId(branch.getId())
                        .eventType("BranchCreated")
                        .payload(objectMapper.writeValueAsString(payload))
                        .version(branch.getVersion())
                        .occurredAt(OffsetDateTime.now())
                        .status(OutboxStatus.READY)
                        .build();

                outboxRepository.save(outbox);
            } catch (Exception e) {
                log.error("Branch 이벤트 생성 실패 (id={}): {}", branch.getId(), e.getMessage());
            }
        }

        log.info("모든 WAREHOUSE Branch 이벤트 Outbox 등록 완료 ({}건)", branches.size());
    }

    /**
     * 모든 거리(BranchVendorDistance) 데이터를 Outbox에 등록
     */
    public void publishAllDistanceEvents() {
        List<BranchVendorDistance> distances = distanceRepository.findAll();

        for (BranchVendorDistance distance : distances) {
            try {
                var branch = distance.getBranch();
                var vendor = distance.getVendor();

                if (branch == null || vendor == null) continue;

                BranchAgencyDistanceEvent.Payload payload = BranchAgencyDistanceEvent.Payload.builder()
                        .distanceId(distance.getId())
                        .branchId(branch.getId())
                        .agencyId(vendor.getId())
                        .distanceKm(distance.getDistanceKm())
                        .deleted(false)
                        .build();

                Outbox outbox = Outbox.builder()
                        .aggregateType("DISTANCE")
                        .aggregateId(distance.getId())
                        .eventType("DistanceCalculated")
                        .payload(objectMapper.writeValueAsString(payload))
                        .version(distance.getVersion())
                        .occurredAt(OffsetDateTime.now())
                        .status(OutboxStatus.READY)
                        .build();

                outboxRepository.save(outbox);
            } catch (Exception e) {
                log.error("Distance 이벤트 생성 실패 (id={}): {}", distance.getId(), e.getMessage());
            }
        }

        log.info("모든 Distance 이벤트 Outbox 등록 완료 ({}건)", distances.size());
    }
}
