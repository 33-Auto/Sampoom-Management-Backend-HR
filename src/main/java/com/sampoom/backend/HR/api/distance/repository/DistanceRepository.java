package com.sampoom.backend.HR.api.distance.repository;

import com.sampoom.backend.HR.api.branch.entity.Branch;
import com.sampoom.backend.HR.api.vendor.entity.Vendor;
import com.sampoom.backend.HR.api.distance.entity.BranchVendorDistance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DistanceRepository extends JpaRepository<BranchVendorDistance, Long> {

    Optional<BranchVendorDistance> findByBranchAndVendor(Branch branch, Vendor vendor);
}
