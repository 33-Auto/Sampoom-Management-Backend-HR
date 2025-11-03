package com.sampoom.backend.HR.api.distance.service;

import com.sampoom.backend.HR.api.branch.entity.Branch;
import com.sampoom.backend.HR.api.branch.repository.BranchRepository;
import com.sampoom.backend.HR.api.distance.entity.BranchVendorDistance;
import com.sampoom.backend.HR.api.vendor.entity.Vendor;
import com.sampoom.backend.HR.api.vendor.repository.VendorRepository;
import com.sampoom.backend.HR.api.distance.repository.DistanceRepository;
import com.sampoom.backend.HR.common.util.DistanceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DistanceService {

    private final DistanceRepository distanceRepository;
    private final BranchRepository branchRepository;
    private final VendorRepository vendorRepository;

    @Transactional
    public void updateDistancesForNewVendor(Vendor vendor) {
        List<Branch> branches = branchRepository.findAll();

        for (Branch branch : branches) {
            if (branch.getLatitude() != null && branch.getLongitude() != null
                    && vendor.getLatitude() != null && vendor.getLongitude() != null) {

                double distance = DistanceUtil.calculateDistance(
                        branch.getLatitude(), branch.getLongitude(),
                        vendor.getLatitude(), vendor.getLongitude()
                );

                BranchVendorDistance distanceEntity = BranchVendorDistance.builder()
                        .branch(branch)
                        .vendor(vendor)
                        .distanceKm(distance)
                        .build();

                distanceRepository.save(distanceEntity);
            }
        }
    }

    @Transactional
    public void updateDistancesForNewBranch(Branch branch) {
        List<Vendor> vendors = vendorRepository.findAll();

        for (Vendor vendor : vendors) {
            if (branch.getLatitude() != null && branch.getLongitude() != null
                    && vendor.getLatitude() != null && vendor.getLongitude() != null) {

                double distance = DistanceUtil.calculateDistance(
                        branch.getLatitude(), branch.getLongitude(),
                        vendor.getLatitude(), vendor.getLongitude()
                );

                BranchVendorDistance distanceEntity = BranchVendorDistance.builder()
                        .branch(branch)
                        .vendor(vendor)
                        .distanceKm(distance)
                        .build();

                distanceRepository.save(distanceEntity);
            }
        }
    }
}
