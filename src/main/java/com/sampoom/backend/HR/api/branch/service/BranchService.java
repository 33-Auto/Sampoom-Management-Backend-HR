package com.sampoom.backend.HR.api.branch.service;

import com.sampoom.backend.HR.api.branch.dto.BranchListResponseDTO;
import com.sampoom.backend.HR.api.branch.dto.BranchRequestDTO;
import com.sampoom.backend.HR.api.branch.dto.BranchResponseDTO;
import com.sampoom.backend.HR.api.branch.dto.BranchUpdateRequestDTO;
import com.sampoom.backend.HR.api.branch.entity.Branch;
import com.sampoom.backend.HR.api.branch.entity.BranchStatus;
import com.sampoom.backend.HR.api.branch.entity.BranchType;
import com.sampoom.backend.HR.api.branch.repository.BranchRepository;
import com.sampoom.backend.HR.api.distance.service.DistanceService;
import com.sampoom.backend.HR.common.dto.PageResponseDTO;
import com.sampoom.backend.HR.common.exception.NotFoundException;
import com.sampoom.backend.HR.common.response.ErrorStatus;
import com.sampoom.backend.HR.common.util.GeoUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final DistanceService distanceService;
    private final GeoUtil geoUtil;

    // 지점 등록
    @Transactional
    public BranchResponseDTO createBranch(BranchRequestDTO branchRequestDTO) {
        String nextCode = generateNextBranchCode(branchRequestDTO.getType());
        Branch branch = branchRequestDTO.toEntity(nextCode);

        // 주소 기반 위경도 자동 설정
        if (branch.getAddress() != null && !branch.getAddress().isBlank()) {
            double[] coords = geoUtil.getLatLngFromAddress(branch.getAddress());
            branch.setLatitude(coords[0]);
            branch.setLongitude(coords[1]);
        }

        Branch saved = branchRepository.save(branch);

        // 거리 자동 계산
        if (saved.getLatitude() != null && saved.getLongitude() != null) {
            distanceService.updateDistancesForNewBranch(saved);
        }

        // 창고일 때만 Outbox 이벤트 발행
        if (saved.isWarehouse()) {
            distanceService.publishBranchEventIfWarehouse(saved, "BranchCreated");
        }

        return BranchResponseDTO.from(saved);
    }

    // 지점 수정
    @Transactional
    public BranchResponseDTO updateBranch(Long id, BranchUpdateRequestDTO branchUpdateRequestDTO) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BRANCH_NOT_FOUND));

        branch.updateInfo(branchUpdateRequestDTO.getName(), branchUpdateRequestDTO.getAddress(), branchUpdateRequestDTO.getStatus());

        // 주소 변경 시 위경도 다시 계산
        if (branchUpdateRequestDTO.getAddress() != null) {
            String addr = branchUpdateRequestDTO.getAddress();
            if (addr.isBlank()) {
                branch.setLatitude(null);
                branch.setLongitude(null);
            } else {
                double[] coords = geoUtil.getLatLngFromAddress(addr);
                branch.setLatitude(coords[0]);
                branch.setLongitude(coords[1]);
            }
        }

        Branch updated = branchRepository.save(branch);

        // 거리 재계산
        if (updated.getLatitude() != null && updated.getLongitude() != null) {
            distanceService.updateDistancesForNewBranch(updated);
        }

        // 창고일 때만 Outbox 이벤트 발행
        if (updated.isWarehouse()) {
            distanceService.publishBranchEventIfWarehouse(updated, "BranchUpdated");
        }

        return BranchResponseDTO.from(updated);
    }

    // 지점 삭제 (비활성화)
    @Transactional
    public void deactivateBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BRANCH_NOT_FOUND));

        branch.deactivate();
        branchRepository.save(branch);
    }

    // 지점 단일 조회
    @Transactional
    public BranchResponseDTO getBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BRANCH_NOT_FOUND));
        return BranchResponseDTO.from(branch);
    }

    // 전체 목록 조회
    @Transactional
    public List<BranchListResponseDTO> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(BranchListResponseDTO::from)
                .toList();
    }

    // 코드 생성
    private String generateNextBranchCode(BranchType type) {
        String prefix = switch (type) {
            case WAREHOUSE -> "WH";
            case FACTORY -> "FC";
        };

        String lastCode = branchRepository.findTopByTypeOrderByIdDesc(type)
                .map(Branch::getBranchCode)
                .orElse(prefix + "-000");

        int number = Integer.parseInt(lastCode.substring(lastCode.lastIndexOf("-") + 1)) + 1;
        return String.format("%s-%03d", prefix, number);
    }

    // 검색
    @Transactional
    public PageResponseDTO<BranchListResponseDTO> searchBranches(
            String keyword,
            BranchType type,
            BranchStatus status,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        String searchKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Page<Branch> branchPage = branchRepository.findByFilters(searchKeyword, type, status, pageable);

        return PageResponseDTO.<BranchListResponseDTO>builder()
                .content(branchPage.getContent().stream()
                        .map(BranchListResponseDTO::from)
                        .collect(Collectors.toList()))
                .totalElements(branchPage.getTotalElements())
                .totalPages(branchPage.getTotalPages())
                .currentPage(branchPage.getNumber())
                .pageSize(branchPage.getSize())
                .build();
    }
}
