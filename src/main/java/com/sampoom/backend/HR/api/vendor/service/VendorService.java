package com.sampoom.backend.HR.api.vendor.service;

import com.sampoom.backend.HR.api.distance.service.DistanceService;
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
import com.sampoom.backend.HR.common.util.GeoUtil;
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
    private final DistanceService distanceService;
    private final GeoUtil geoUtil;

    // 거래처 등록
    @Transactional
    public VendorResponseDTO createVendor(VendorRequestDTO vendorRequestDTO) {
        String nextCode = generateNextVendorCode();

        Vendor vendor = vendorRequestDTO.toEntity(nextCode);

        // 주소로 위경도 자동 설정
        if (vendor.getAddress() != null && !vendor.getAddress().isBlank()) {
            double[] coords = geoUtil.getLatLngFromAddress(vendor.getAddress());
            vendor.setLatitude(coords[0]);
            vendor.setLongitude(coords[1]);
        }

        Vendor saved = vendorRepository.save(vendor);

        // 거리 계산
        if (saved.getLatitude() != null && saved.getLongitude() != null) {
            distanceService.updateDistancesForNewVendor(saved);
        }

        return VendorResponseDTO.from(saved);
    }

    // 거래처 수정
    @Transactional
    public VendorResponseDTO updateVendor(Long id, VendorUpdateRequestDTO dto) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.VENDOR_NOT_FOUND));

        vendor.changeInfo(dto.getName(), dto.getBusinessNumber(), dto.getCeoName(),
                dto.getAddress(), dto.getStatus());

        // 주소가 바뀌었으면 다시 위경도 계산
        if (dto.getAddress() != null) {
            String newAddress = dto.getAddress();
            if (newAddress.isBlank()) {
                vendor.setLatitude(null);
                vendor.setLongitude(null);
            } else {
                double[] coords = geoUtil.getLatLngFromAddress(newAddress);
                vendor.setLatitude(coords[0]);
                vendor.setLongitude(coords[1]);
            }
        }

        Vendor updated = vendorRepository.save(vendor);

        if (updated.getLatitude() != null && updated.getLongitude() != null) {
            distanceService.updateDistancesForNewVendor(updated);
        }

        return VendorResponseDTO.from(updated);
    }

    @Transactional
    public void deleteVendor(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.VENDOR_NOT_FOUND));

        vendor.deactivate();
        vendorRepository.save(vendor);
    }

    @Transactional(readOnly = true)
    public VendorResponseDTO getVendor(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.VENDOR_NOT_FOUND));
        return VendorResponseDTO.from(vendor);
    }

    @Transactional(readOnly = true)
    public List<VendorListResponseDTO> getAllVendors() {
        return vendorRepository.findAll().stream()
                .map(VendorListResponseDTO::from)
                .toList();
    }

    private String generateNextVendorCode() {
        String prefix = "AGC";

        String lastCode = vendorRepository.findTopByOrderByIdDesc()
                .map(Vendor::getVendorCode)
                .orElse(prefix + "-000");

        int number = Integer.parseInt(lastCode.substring(lastCode.lastIndexOf("-") + 1)) + 1;
        return String.format("%s-%03d", prefix, number);
    }

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
