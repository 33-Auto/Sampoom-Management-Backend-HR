package com.sampoom.backend.HR.api.distance.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchEventBatchRunner implements CommandLineRunner {

    private final BranchEventBatchService branchEventBatchService;

    @Override
    public void run(String... args) {
        // 필요할 때만 실행 (DB 전체 Outbox 초기 등록)
//         branchEventBatchService.publishAllBranchEvents();
//         branchEventBatchService.publishAllDistanceEvents();
//         branchEventBatchService.publishAllFactoryEvents();
//         branchEventBatchService.publishAllVendorEvents();
    }
}
