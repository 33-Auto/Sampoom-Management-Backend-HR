package com.sampoom.backend.HR.api.branch.repository;

import com.sampoom.backend.HR.api.branch.entity.Branch;
import com.sampoom.backend.HR.api.branch.entity.BranchStatus;
import com.sampoom.backend.HR.api.branch.entity.BranchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findTopByTypeOrderByIdDesc(BranchType type);

    @Query("""
    SELECT b FROM Branch b
    WHERE (:keyword IS NULL 
        OR b.branchCode LIKE CONCAT('%', CAST(:keyword AS string), '%') 
        OR b.name LIKE CONCAT('%', CAST(:keyword AS string), '%'))
    AND (:type IS NULL OR b.type = :type)
    AND (:status IS NULL OR b.status = :status)
    ORDER BY b.createdAt DESC
""")
    Page<Branch> findByFilters(
            @Param("keyword") String keyword,
            @Param("type") BranchType type,
            @Param("status") BranchStatus status,
            Pageable pageable
    );
}
