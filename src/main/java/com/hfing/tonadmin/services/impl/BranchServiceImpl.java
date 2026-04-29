package com.hfing.tonadmin.services.impl;

import com.hfing.tonadmin.dto.request.BranchRequest;
import com.hfing.tonadmin.entities.Branch;
import com.hfing.tonadmin.mappers.BranchMapper;
import com.hfing.tonadmin.repositories.BranchRepository;
import com.hfing.tonadmin.services.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    @Override
    public List<Branch> getAllBranches() {
        return branchRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Branch getBranchById(String id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh"));
    }

    @Override
    @Transactional
    public boolean createBranch(BranchRequest request, BindingResult bindingResult) {
        String code = normalizeCode(request.code());

        if (branchRepository.existsByCode(code)) {
            bindingResult.rejectValue("code", "duplicate", "Mã chi nhánh đã tồn tại");
            return false;
        }

        Branch branch = branchMapper.toBranch(request);
        branch.setCode(code);
        branch.setName(trimToNull(request.name()));
        branch.setAddress(trimToNull(request.address()));
        branch.setPhone(trimToNull(request.phone()));
        branch.setActive(true);

        branchRepository.save(branch);
        return true;
    }

    @Override
    @Transactional
    public boolean updateBranch(String id, BranchRequest request, BindingResult bindingResult) {
        Branch branch = getBranchById(id);
        String code = normalizeCode(request.code());

        if (branchRepository.existsByCodeAndIdNot(code, id)) {
            bindingResult.rejectValue("code", "duplicate", "Mã chi nhánh đã tồn tại");
            return false;
        }

        branchMapper.updateBranchFromRequest(request, branch);
        branch.setCode(code);
        branch.setName(trimToNull(request.name()));
        branch.setAddress(trimToNull(request.address()));
        branch.setPhone(trimToNull(request.phone()));

        branchRepository.save(branch);
        return true;
    }

    @Override
    @Transactional
    public void toggleActive(String id) {
        Branch branch = getBranchById(id);
        branch.setActive(!Boolean.TRUE.equals(branch.getActive()));
        branchRepository.save(branch);
    }



    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
