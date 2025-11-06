package com.sampoom.backend.HR.api.branch.controller;

import com.sampoom.backend.HR.api.branch.dto.*;
import com.sampoom.backend.HR.api.branch.entity.BranchStatus;
import com.sampoom.backend.HR.api.branch.entity.BranchType;
import com.sampoom.backend.HR.api.branch.service.BranchService;
import com.sampoom.backend.HR.common.dto.PageResponseDTO;
import com.sampoom.backend.HR.common.response.ApiResponse;
import com.sampoom.backend.HR.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
@Tag(name = "지점 관리 API", description = "공장/창고 지점 등록, 수정, 조회, 검색, 비활성화")
public class BranchController {

    private final BranchService branchService;

    /** 지점 등록 */
    @Operation(summary = "지점 등록", description = "새로운 지점(공장/창고)을 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<BranchResponseDTO>> createBranch(@Valid @RequestBody BranchRequestDTO dto) {
        BranchResponseDTO response = branchService.createBranch(dto);
        return ApiResponse.success(SuccessStatus.BRANCH_CREATED, response);

    }

    /** 지점 수정 */
    @Operation(summary = "지점 수정", description = "지점 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponseDTO>> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody BranchUpdateRequestDTO dto
    ) {
        BranchResponseDTO response = branchService.updateBranch(id, dto);
        return ApiResponse.success(SuccessStatus.BRANCH_UPDATED, response);

    }

    /**  지점 비활성화 (삭제 대신) */
    @Operation(summary = "지점 비활성화(삭제)", description = "지점을 비활성화(삭제 처리)합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateBranch(@PathVariable Long id) {
        branchService.deactivateBranch(id);
        return ApiResponse.success_only(SuccessStatus.OK);
    }

    /** 지점 상세 조회 */
    @Operation(summary = "지점 상세 조회", description = "지점 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponseDTO>> getBranch(@PathVariable Long id) {
        BranchResponseDTO response = branchService.getBranch(id);
        return ApiResponse.success(SuccessStatus.OK, response);
    }

    /** 지점 전체 목록 조회 */
    @Operation(summary = "지점 전체 목록 조회", description = "모든 지점(공장/창고)을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchListResponseDTO>>> getAllBranches() {
        List<BranchListResponseDTO> list = branchService.getAllBranches();
        return ApiResponse.success(SuccessStatus.OK, list);
    }

    /** 지점 검색 */
    @Operation(summary = "지점 검색", description = "이름, 유형, 상태 등으로 지점을 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponseDTO<BranchListResponseDTO>>> searchBranches(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BranchType type,
            @RequestParam(required = false) BranchStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponseDTO<BranchListResponseDTO> result =
                branchService.searchBranches(keyword, type, status, page, size);
        return ApiResponse.success(SuccessStatus.OK, result);
    }
}
