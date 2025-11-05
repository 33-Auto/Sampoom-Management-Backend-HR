package com.sampoom.backend.HR.api.distance.repository;

import com.sampoom.backend.HR.api.branch.entity.Branch;
import com.sampoom.backend.HR.api.distance.entity.BranchFactoryDistance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchFactoryDistanceRepository extends JpaRepository<BranchFactoryDistance, Long> {

    Optional<BranchFactoryDistance> findByBranchAndFactory(Branch branch, Branch factory);

    List<BranchFactoryDistance> findByBranch(Branch branch);

    List<BranchFactoryDistance> findByFactory(Branch factory);
}
