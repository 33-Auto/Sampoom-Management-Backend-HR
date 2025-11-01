package com.sampoom.backend.HR.api.vendor.repository;

import com.sampoom.backend.HR.api.vendor.entity.Vendor;
import com.sampoom.backend.HR.api.vendor.entity.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findTopByOrderByIdDesc();

    @Query("""
    SELECT v FROM Vendor v
    WHERE (COALESCE(:keyword, '') = '' 
           OR v.vendorCode ILIKE CONCAT('%', :keyword, '%') 
           OR v.name ILIKE CONCAT('%', :keyword, '%'))
    AND (:status IS NULL OR v.status = :status)
    ORDER BY v.createdAt DESC
""")
    Page<Vendor> findByFilters(
            @Param("keyword") String keyword,
            @Param("status") VendorStatus status,
            Pageable pageable
    );
}
