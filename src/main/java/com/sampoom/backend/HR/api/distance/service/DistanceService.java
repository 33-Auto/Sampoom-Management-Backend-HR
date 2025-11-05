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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistanceService {

    private final DistanceRepository distanceRepository;
    private final BranchRepository branchRepository;
    private final VendorRepository vendorRepository;
    private final OutboxService outboxService;
    private final BranchFactoryDistanceService branchFactoryDistanceService;

    @Transactional
    public void updateDistancesForNewVendor(Vendor vendor) {
        // 창고만 조회 (공장 제외)
        List<Branch> warehouses = branchRepository.findByType(BranchType.WAREHOUSE);

        for (Branch warehouse : warehouses) {
            if (warehouse.getLatitude() != null && warehouse.getLongitude() != null
                    && vendor.getLatitude() != null && vendor.getLongitude() != null) {

                Double distanceKm = DistanceUtil.calculateDistance(
                        warehouse.getLatitude(), warehouse.getLongitude(),
                        vendor.getLatitude(), vendor.getLongitude()
                );

                BranchVendorDistance distanceEntity = distanceRepository.findByBranchAndVendor(warehouse, vendor)
                        .orElseGet(() -> BranchVendorDistance.builder()
                                .branch(warehouse)
                                .vendor(vendor)
                                .build());

                distanceEntity.updateDistance(distanceKm);
                distanceRepository.save(distanceEntity);

                // Payload 생성
                BranchAgencyDistanceEvent.Payload payload = BranchAgencyDistanceEvent.Payload.builder()
                        .distanceId(distanceEntity.getId())
                        .branchId(warehouse.getId())
                        .agencyId(vendor.getId())
                        .distanceKm(distanceKm)
                        .deleted(false)
                        .build();

                // Outbox 저장
                outboxService.saveEvent(
                        "BRANCH_AGENCY_DISTANCE",  // 새로운 aggregate type
                        distanceEntity.getId(),
                        "DistanceCalculated",
                        distanceEntity.getVersion(),
                        payload
                );
            }
        }
        log.info("[DistanceService] Vendor({}) 거리 {}건 업데이트 및 이벤트 발행 완료",
                vendor.getName(), warehouses.size());
    }

    @Transactional
    public void updateDistancesForNewBranch(Branch branch) {

        // 공장인 경우 창고-공장 거리 계산
        if (branch.getType() == BranchType.FACTORY) {
            branchFactoryDistanceService.updateDistancesForNewFactory(branch);
            return;
        }

        // 창고인 경우 대리점-창고 거리 계산
        if (branch.getType() == BranchType.WAREHOUSE) {
            // 기존 대리점-창고 거리 계산
            updateVendorDistancesForWarehouse(branch);
            // 창고-공장 거리 계산
            branchFactoryDistanceService.updateDistancesForNewWarehouse(branch);
        }
    }

    /**
     * 창고에 대한 대리점-창고 거리 계산
     */
    private void updateVendorDistancesForWarehouse(Branch warehouse) {
        List<Vendor> vendors = vendorRepository.findAll();

        for (Vendor vendor : vendors) {
            if (warehouse.getLatitude() != null && warehouse.getLongitude() != null
                    && vendor.getLatitude() != null && vendor.getLongitude() != null) {

                double distanceKm = DistanceUtil.calculateDistance(
                        warehouse.getLatitude(), warehouse.getLongitude(),
                        vendor.getLatitude(), vendor.getLongitude()
                );

                BranchVendorDistance distanceEntity = distanceRepository.findByBranchAndVendor(warehouse, vendor)
                        .orElseGet(() -> BranchVendorDistance.builder()
                                .branch(warehouse)
                                .vendor(vendor)
                                .build());

                distanceEntity.updateDistance(distanceKm);
                distanceRepository.save(distanceEntity);

                // Payload 생성
                BranchAgencyDistanceEvent.Payload payload = BranchAgencyDistanceEvent.Payload.builder()
                        .distanceId(distanceEntity.getId())
                        .branchId(warehouse.getId())
                        .agencyId(vendor.getId())
                        .distanceKm(distanceKm)
                        .deleted(false)
                        .build();

                // Outbox 저장
                outboxService.saveEvent(
                        "BRANCH_AGENCY_DISTANCE",  // 새로운 aggregate type
                        distanceEntity.getId(),
                        "DistanceCalculated",
                        distanceEntity.getVersion(),
                        payload
                );
            }
        }
    }

    @Transactional
    public void publishBranchEvent(Branch branch, String eventType) {
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

        String aggregateType = branch.getType() == BranchType.FACTORY ? "FACTORY" : "BRANCH";

        outboxService.saveEvent(
                aggregateType,
                branch.getId(),
                eventType,
                branch.getVersion(),
                payload
        );

        log.info("[DistanceService] {} 이벤트 발행 완료: {} ({})",
                aggregateType, branch.getName(), eventType);
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

    @Transactional
    public void publishBranchEventIfFactory(Branch branch, String eventType) {
        var payload = BranchEvent.Payload.builder()
                .branchId(branch.getId())
                .branchCode(branch.getBranchCode())
                .branchName(branch.getName())
                .address(branch.getAddress())
                .latitude(branch.getLatitude())
                .longitude(branch.getLongitude())
                .status(branch.getStatus().name())
                .deleted(false)
                .build();

        outboxService.saveEvent("FACTORY", branch.getId(), eventType, branch.getVersion(), payload);
        log.info("[DistanceService] Factory 이벤트 발행 완료: {} ({})", branch.getName(), eventType);
    }
}
