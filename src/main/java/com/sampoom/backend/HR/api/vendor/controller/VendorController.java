package com.sampoom.backend.HR.api.vendor.controller;


import com.sampoom.backend.HR.api.vendor.dto.VendorListResponseDTO;
import com.sampoom.backend.HR.api.vendor.dto.VendorRequestDTO;
import com.sampoom.backend.HR.api.vendor.dto.VendorResponseDTO;
import com.sampoom.backend.HR.api.vendor.dto.VendorUpdateRequestDTO;
import com.sampoom.backend.HR.api.vendor.entity.VendorStatus;
import com.sampoom.backend.HR.api.vendor.service.VendorService;
import com.sampoom.backend.HR.common.dto.PageResponseDTO;
import com.sampoom.backend.HR.common.response.ApiResponse;
import com.sampoom.backend.HR.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vendors")
@Tag(name = "Vendor API", description = "거래처 마스터 관리 API")
public class VendorController {

    private final VendorService vendorService;

    /**
     * 거래처 등록
     */
    @Operation(summary = "거래처 등록", description = "거래처 유형에 따라 코드가 자동 생성됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<VendorResponseDTO>> createVendor(@RequestBody VendorRequestDTO dto) {
        VendorResponseDTO response = vendorService.createVendor(dto);
        return ApiResponse.success(SuccessStatus.CREATED, response);
    }

    @Operation(summary = "거래처 수정", description = "기존 거래처 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorResponseDTO>> updateVendor(
            @PathVariable Long id,
            @RequestBody VendorUpdateRequestDTO dto) {
        return ApiResponse.success(SuccessStatus.OK, vendorService.updateVendor(id, dto));
    }

    @Operation(summary = "거래처 삭제", description = "거래처를 비활성화합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVendor(@PathVariable Long id) {
        vendorService.deleteVendor(id);
        return ApiResponse.success(SuccessStatus.OK, null);
    }

    /**
     * 거래처 전체 조회
     */
    @Operation(summary = "거래처 전체 조회", description = "전체 거래처 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VendorListResponseDTO>>> getAllVendors() {
        List<VendorListResponseDTO> list = vendorService.getAllVendors();
        return ApiResponse.success(SuccessStatus.OK, list);
    }

    /**
     * 거래처 단건 조회 (ID 기준)
     */
    @Operation(summary = "거래처 상세 조회", description = "거래처 ID로 단건 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorResponseDTO>> getVendor(@PathVariable Long id) {
        VendorResponseDTO response = vendorService.getVendor(id);
        return ApiResponse.success(SuccessStatus.OK, response);
    }

    @Operation(summary = "거래처 검색", description = "거래처 코드, 이름, 유형, 상태로 검색 및 페이징 처리합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponseDTO<VendorListResponseDTO>>> searchVendors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) VendorStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponseDTO<VendorListResponseDTO> response = vendorService.searchVendors(keyword, status, page, size);
        return ApiResponse.success(SuccessStatus.OK, response);
    }

}
