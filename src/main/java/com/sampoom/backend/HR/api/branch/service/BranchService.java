package com.sampoom.backend.HR.api.branch.service;

import com.sampoom.backend.HR.api.branch.dto.BranchListResponseDTO;
import com.sampoom.backend.HR.api.branch.dto.BranchRequestDTO;
import com.sampoom.backend.HR.api.branch.dto.BranchResponseDTO;
import com.sampoom.backend.HR.api.branch.dto.BranchUpdateRequestDTO;
import com.sampoom.backend.HR.api.branch.entity.Branch;
import com.sampoom.backend.HR.api.branch.entity.BranchStatus;
import com.sampoom.backend.HR.api.branch.entity.BranchType;
import com.sampoom.backend.HR.api.branch.repository.BranchRepository;
import com.sampoom.backend.HR.common.dto.PageResponseDTO;
import com.sampoom.backend.HR.common.exception.NotFoundException;
import com.sampoom.backend.HR.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.sampoom.backend.HR.api.branch.entity.BranchType.FACTORY;
import static com.sampoom.backend.HR.api.branch.entity.BranchType.WAREHOUSE;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    // 지점 등록
    @Transactional
    public BranchResponseDTO createBranch(BranchRequestDTO branchRequestDTO) {
        String nextCode = generateNextBranchCode(branchRequestDTO.getType());
        Branch branch = branchRequestDTO.toEntity(nextCode);
        Branch saved = branchRepository.save(branch);
        return BranchResponseDTO.from(saved);
    }

    // 지점 수정
    @Transactional
    public BranchResponseDTO updateBranch(Long id, BranchUpdateRequestDTO branchUpdateRequestDTO) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BRANCH_NOT_FOUND));

        branch.updateInfo(branchUpdateRequestDTO.getName(), branchUpdateRequestDTO.getAddress(), branchUpdateRequestDTO.getStatus());
        Branch updated = branchRepository.save(branch);

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
    // 🔹 전체 목록 조회
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
