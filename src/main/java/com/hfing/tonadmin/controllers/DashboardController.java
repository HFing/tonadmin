package com.hfing.tonadmin.controllers;

import com.hfing.tonadmin.dto.response.DashboardStats;
import com.hfing.tonadmin.repositories.BranchRepository;
import com.hfing.tonadmin.services.CurrentUserService;
import com.hfing.tonadmin.services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUserService;
    private final BranchRepository branchRepository;

    @GetMapping({"/", "/dashboard"})
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_STAFF')")
    public String dashboard(
            @RequestParam(required = false) String branchId,
            Model model
    ) {
        boolean isAdmin = currentUserService.isAdmin();

        DashboardStats stats = dashboardService.getDashboardStats(branchId);

        model.addAttribute("stats", stats);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("selectedBranchId", branchId);

        if (isAdmin) {
            model.addAttribute("branches", branchRepository.findByActiveTrueOrderByNameAsc());
        } else {
            model.addAttribute("staffBranch", currentUserService.getCurrentUserBranch());
        }

        return "index";
    }
}