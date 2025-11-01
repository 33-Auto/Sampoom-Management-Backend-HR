package com.sampoom.backend.HR.api.vendor.service;

import com.sampoom.backend.HR.api.vendor.dto.VendorListResponseDTO;
import com.sampoom.backend.HR.api.vendor.dto.VendorRequestDTO;
import com.sampoom.backend.HR.api.vendor.dto.VendorResponseDTO;
import com.sampoom.backend.HR.api.vendor.dto.VendorUpdateRequestDTO;
import com.sampoom.backend.HR.api.vendor.entity.Vendor;
import com.sampoom.backend.HR.api.vendor.entity.VendorStatus;
import com.sampoom.backend.HR.api.vendor.repository.VendorRepository;
import com.sampoom.backend.HR.common.dto.PageResponseDTO;
import com.sampoom.backend.HR.common.exception.NotFoundException;
import com.sampoom.backend.HR.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;

    // 거래처 등록
    @Transactional
    public VendorResponseDTO createVendor(VendorRequestDTO vendorRequestDTO) {
        String nextCode = generateNextVendorCode();
        Vendor vendor = vendorRequestDTO.toEntity(nextCode);
        Vendor saved = vendorRepository.save(vendor);
        return VendorResponseDTO.from(saved);
    }

    // 거래처 수정
    @Transactional
    public VendorResponseDTO updateVendor(Long id, VendorUpdateRequestDTO dto) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.VENDOR_NOT_FOUND));

        vendor.changeInfo(
                dto.getName(),
                dto.getBusinessNumber(),
                dto.getCeoName(),
                dto.getAddress(),
                dto.getStatus()
        );

        Vendor updated = vendorRepository.save(vendor);
        return VendorResponseDTO.from(updated);
    }

    @Transactional
    public void deleteVendor(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.VENDOR_NOT_FOUND));

        // 실제 삭제 대신 비활성화
        vendor.deactivate();
        vendorRepository.save(vendor);
    }

    // 거래처 조회
    @Transactional
    public VendorResponseDTO getVendor(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.VENDOR_NOT_FOUND));
        return VendorResponseDTO.from(vendor);
    }

    // 거래처 목록 조회
    @Transactional
    public List<VendorListResponseDTO> getAllVendors() {
        return vendorRepository.findAll().stream()
                .map(VendorListResponseDTO::from)
                .toList();
    }

    // 거래처 코드 생성
    private String generateNextVendorCode() {
        String prefix = "AGC";

        String lastCode = vendorRepository.findTopByOrderByIdDesc()
                .map(Vendor::getVendorCode)
                .orElse(prefix + "-000");

        int number = Integer.parseInt(lastCode.substring(lastCode.lastIndexOf("-") + 1)) + 1;
        return String.format("%s-%03d", prefix, number);
    }

    // 거래처 검색
    @Transactional(readOnly = true)
    public PageResponseDTO<VendorListResponseDTO> searchVendors(
            String keyword,
            VendorStatus status,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        String searchKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Page<Vendor> vendorPage = vendorRepository.findByFilters(searchKeyword, status, pageable);

        return PageResponseDTO.<VendorListResponseDTO>builder()
                .content(vendorPage.getContent().stream()
                        .map(VendorListResponseDTO::from)
                        .collect(Collectors.toList()))
                .totalElements(vendorPage.getTotalElements())
                .totalPages(vendorPage.getTotalPages())
                .currentPage(vendorPage.getNumber())
                .pageSize(vendorPage.getSize())
                .build();
    }
}
