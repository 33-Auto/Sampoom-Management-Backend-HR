package com.sampoom.backend.HR.api.vendor.repository;

import com.sampoom.backend.HR.api.vendor.entity.Vendor;
import com.sampoom.backend.HR.api.vendor.entity.VendorStatus;
import com.sampoom.backend.HR.api.vendor.entity.VendorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findTopByTypeOrderByIdDesc(VendorType type);

    @Query("""
        SELECT v FROM Vendor v
        WHERE (:keyword IS NULL OR v.vendorCode LIKE %:keyword% OR v.name LIKE %:keyword%)
        AND (:type IS NULL OR v.type = :type)
        AND (:status IS NULL OR v.status = :status)
        ORDER BY v.createdAt DESC
    """)
    Page<Vendor> findByFilters(
            @Param("keyword") String keyword,
            @Param("type") VendorType type,
            @Param("status") VendorStatus status,
            Pageable pageable
    );
}
