package com.sampoom.backend.HR.api.distance.service;

import com.sampoom.backend.HR.api.branch.entity.Branch;
import com.sampoom.backend.HR.api.branch.entity.BranchType;
import com.sampoom.backend.HR.api.branch.event.dto.BranchEvent;
import com.sampoom.backend.HR.api.branch.repository.BranchRepository;
import com.sampoom.backend.HR.api.distance.entity.BranchVendorDistance;
import com.sampoom.backend.HR.api.distance.event.dto.BranchAgencyDistanceEvent;
import com.sampoom.backend.HR.api.vendor.entity.Vendor;
import com.sampoom.backend.HR.api.vendor.repository.VendorRepository;
import com.sampoom.backend.HR.api.distance.repository.DistanceRepository;
import com.sampoom.backend.HR.common.outbox.service.OutboxService;
import com.sampoom.backend.HR.common.util.DistanceUtil;
import com.sampoom.backend.HR.common.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistanceService {

    private final DistanceRepository distanceRepository;
    private final BranchRepository branchRepository;
    private final VendorRepository vendorRepository;
    private final OutboxService outboxService;

    @Transactional
    public void updateDistancesForNewVendor(Vendor vendor) {
        List<Branch> branches = branchRepository.findAll();

        for (Branch branch : branches) {
            if (branch.getLatitude() != null && branch.getLongitude() != null
                    && vendor.getLatitude() != null && vendor.getLongitude() != null) {

                Double distanceKm = DistanceUtil.calculateDistance(
                        branch.getLatitude(), branch.getLongitude(),
                        vendor.getLatitude(), vendor.getLongitude()
                );

                BranchVendorDistance distanceEntity = distanceRepository.findByBranchAndVendor(branch, vendor)
                        .orElseGet(() -> BranchVendorDistance.builder()
                                .branch(branch)
                                .vendor(vendor)
                                .build());

                distanceEntity.updateDistance(distanceKm);
                distanceRepository.save(distanceEntity);

                // Payload 생성
                BranchAgencyDistanceEvent.Payload payload = BranchAgencyDistanceEvent.Payload.builder()
                        .distanceId(distanceEntity.getId())
                        .branchId(branch.getId())
                        .agencyId(vendor.getId())
                        .distanceKm(distanceKm)
                        .deleted(false)
                        .build();

                // Outbox 저장
                outboxService.saveEvent(
                        "DISTANCE",
                        distanceEntity.getId(),
                        "DistanceCalculated",
                        distanceEntity.getVersion(),
                        payload
                );
            }
        }
        log.info("[DistanceService] Vendor({}) 거리 {}건 업데이트 및 이벤트 발행 완료",
                vendor.getName(), branches.size());
    }

    @Transactional
    public void updateDistancesForNewBranch(Branch branch) {

        // 공장은 거리 계산 안 함
        if (branch.getType() != BranchType.WAREHOUSE) {
            log.info("[DistanceService] 공장은 거리 계산 스킵: {}", branch.getName());
            return;
        }

        List<Vendor> vendors = vendorRepository.findAll();

        for (Vendor vendor : vendors) {
            if (branch.getLatitude() != null && branch.getLongitude() != null
                    && vendor.getLatitude() != null && vendor.getLongitude() != null) {

                double distanceKm = DistanceUtil.calculateDistance(
                        branch.getLatitude(), branch.getLongitude(),
                        vendor.getLatitude(), vendor.getLongitude()
                );

                BranchVendorDistance distanceEntity = distanceRepository.findByBranchAndVendor(branch, vendor)
                        .orElseGet(() -> BranchVendorDistance.builder()
                                .branch(branch)
                                .vendor(vendor)
                                .build());

                distanceEntity.updateDistance(distanceKm);
                distanceRepository.save(distanceEntity);

                ;// Payload 생성
                BranchAgencyDistanceEvent.Payload payload = BranchAgencyDistanceEvent.Payload.builder()
                        .distanceId(distanceEntity.getId())
                        .branchId(branch.getId())
                        .agencyId(vendor.getId())
                        .distanceKm(distanceKm)
                        .deleted(false)
                        .build();

                // Outbox 저장
                outboxService.saveEvent(
                        "DISTANCE",
                        distanceEntity.getId(),
                        "DistanceCalculated",
                        distanceEntity.getVersion(),
                        payload
                );
            }
        }
    }


    @Transactional
    public void publishBranchEventIfWarehouse(Branch branch, String eventType) {
        if (branch.getType() != BranchType.WAREHOUSE) return;

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

        outboxService.saveEvent(
                "BRANCH",
                branch.getId(),
                eventType,
                branch.getVersion(),
                payload
        );

        log.info("[DistanceService] 창고 BRANCH 이벤트 발행 완료: {}", branch.getName());
    }

    private boolean hasValidCoordinates(Branch branch, Vendor vendor) {
        return branch.getLatitude() != null && branch.getLongitude() != null
                && vendor.getLatitude() != null && vendor.getLongitude() != null;
    }
}
