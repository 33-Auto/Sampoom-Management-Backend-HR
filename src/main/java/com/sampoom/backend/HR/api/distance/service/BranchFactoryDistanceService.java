package com.sampoom.backend.HR.api.distance.service;

import com.sampoom.backend.HR.api.branch.entity.Branch;
import com.sampoom.backend.HR.api.branch.entity.BranchType;
import com.sampoom.backend.HR.api.branch.repository.BranchRepository;
import com.sampoom.backend.HR.api.distance.entity.BranchFactoryDistance;
import com.sampoom.backend.HR.api.distance.event.dto.BranchFactoryDistanceEvent;
import com.sampoom.backend.HR.api.distance.repository.BranchFactoryDistanceRepository;
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
public class BranchFactoryDistanceService {

    private final BranchFactoryDistanceRepository branchFactoryDistanceRepository;
    private final BranchRepository branchRepository;
    private final OutboxService outboxService;

    /**
     * 새로운 공장이 생성되었을 때 모든 창고와의 거리 계산
     */
    @Transactional
    public void updateDistancesForNewFactory(Branch factory) {
        if (factory.getType() != BranchType.FACTORY) {
            log.warn("Factory가 아닌 Branch로 거리 계산 요청됨: {}", factory.getId());
            return;
        }

        List<Branch> warehouses = branchRepository.findByType(BranchType.WAREHOUSE);

        for (Branch warehouse : warehouses) {
            calculateAndSaveDistance(warehouse, factory);
        }

        log.info("[BranchFactoryDistanceService] 공장({}) - 창고 거리 {}건 계산 완료",
                factory.getName(), warehouses.size());
    }

    /**
     * 새로운 창고가 생성되었을 때 모든 공장과의 거리 계산
     */
    @Transactional
    public void updateDistancesForNewWarehouse(Branch warehouse) {
        if (warehouse.getType() != BranchType.WAREHOUSE) {
            log.warn("Warehouse가 아닌 Branch로 거리 계산 요청됨: {}", warehouse.getId());
            return;
        }

        List<Branch> factories = branchRepository.findByType(BranchType.FACTORY);

        for (Branch factory : factories) {
            calculateAndSaveDistance(warehouse, factory);
        }

        log.info("[BranchFactoryDistanceService] 창고({}) - 공장 거리 {}건 계산 완료",
                warehouse.getName(), factories.size());
    }

    /**
     * 창고-공장 거리 계산 및 저장
     */
    @Transactional
    public void calculateAndSaveDistance(Branch warehouse, Branch factory) {
        if (warehouse.getLatitude() != null && warehouse.getLongitude() != null
                && factory.getLatitude() != null && factory.getLongitude() != null) {

            Double distanceKm = DistanceUtil.calculateDistance(
                    warehouse.getLatitude(), warehouse.getLongitude(),
                    factory.getLatitude(), factory.getLongitude()
            );

            BranchFactoryDistance distanceEntity = branchFactoryDistanceRepository
                    .findByBranchAndFactory(warehouse, factory)
                    .orElseGet(() -> BranchFactoryDistance.builder()
                            .branch(warehouse)
                            .factory(factory)
                            .build());

            distanceEntity.updateDistance(distanceKm);
            BranchFactoryDistance savedDistance = branchFactoryDistanceRepository.save(distanceEntity);

            // Outbox 이벤트 발행
            publishDistanceEvent(savedDistance);

            log.debug("창고-공장 거리 계산 완료: {}km (창고:{} <-> 공장:{})",
                    distanceKm, warehouse.getName(), factory.getName());
        } else {
            log.warn("위도/경도 정보 부족으로 거리 계산 불가: 창고:{}, 공장:{}",
                    warehouse.getName(), factory.getName());
        }
    }

    /**
     * 창고-공장 거리 이벤트 발행
     */
    private void publishDistanceEvent(BranchFactoryDistance distance) {
        BranchFactoryDistanceEvent.Payload payload = BranchFactoryDistanceEvent.Payload.builder()
                .distanceId(distance.getId())
                .branchId(distance.getBranch().getId())
                .factoryId(distance.getFactory().getId())
                .distanceKm(distance.getDistanceKm())
                .branchName(distance.getBranch().getName())
                .factoryName(distance.getFactory().getName())
                .build();

        outboxService.saveEvent(
                "BRANCH_FACTORY_DISTANCE",  // 새로운 aggregate type
                distance.getId(),
                "BranchFactoryDistanceCalculated",
                distance.getVersion(),
                payload
        );

        log.debug("창고-공장 거리 이벤트 발행 완료: distanceId={}", distance.getId());
    }

    /**
     * 모든 창고-공장 거리 일괄 재계산
     */
    @Transactional
    public void recalculateAllDistances() {
        List<Branch> warehouses = branchRepository.findByType(BranchType.WAREHOUSE);
        List<Branch> factories = branchRepository.findByType(BranchType.FACTORY);

        int totalCalculations = warehouses.size() * factories.size();
        log.info("창고-공장 거리 일괄 재계산 시작: {}개 계산 예정", totalCalculations);

        for (Branch warehouse : warehouses) {
            for (Branch factory : factories) {
                calculateAndSaveDistance(warehouse, factory);
            }
        }

        log.info("창고-공장 거리 일괄 재계산 완료");
    }
}
