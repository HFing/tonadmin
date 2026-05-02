package com.hfing.tonadmin.services.impl;

import com.hfing.tonadmin.entities.Branch;
import com.hfing.tonadmin.entities.User;
import com.hfing.tonadmin.repositories.UserRepository;
import com.hfing.tonadmin.services.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserRepository userRepository;

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Người dùng chưa đăng nhập");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng hiện tại"));
    }

    @Override
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    @Override
    public boolean isBranchStaff() {
        return hasRole("ROLE_BRANCH_STAFF");
    }

    @Override
    public Branch getCurrentUserBranch() {
        User currentUser = getCurrentUser();

        if (currentUser.getBranch() == null) {
            throw new IllegalStateException("Nhân viên chưa được gán chi nhánh");
        }

        return currentUser.getBranch();
    }

    @Override
    public boolean canAccessBranch(String branchId) {
        if (isAdmin()) {
            return true;
        }

        Branch branch = getCurrentUserBranch();

        return branch != null && branch.getId().equals(branchId);
    }

    private boolean hasRole(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals(roleName));
    }
}