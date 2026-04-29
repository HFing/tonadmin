package com.hfing.tonadmin.controllers;

import com.hfing.tonadmin.dto.BreadcrumbItem;
import com.hfing.tonadmin.dto.request.BranchRequest;
import com.hfing.tonadmin.entities.Branch;
import com.hfing.tonadmin.mappers.BranchMapper;
import com.hfing.tonadmin.services.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/branches")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BranchController {

    private final BranchService branchService;
    private final BranchMapper branchMapper;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("branches", branchService.getAllBranches());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Chi nhánh", null)
        ));

        return "branches/index";
    }

    @GetMapping("/create")
    public String createPage(Model model) {
        model.addAttribute("branchRequest", new BranchRequest("", "", "", ""));

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Chi nhánh", "/branches"),
                new BreadcrumbItem("Thêm mới", null)
        ));

        return "branches/create";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("branchRequest") BranchRequest branchRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addCreateBreadcrumb(model);
            return "branches/create";
        }

        boolean success = branchService.createBranch(branchRequest, bindingResult);

        if (!success) {
            addCreateBreadcrumb(model);
            return "branches/create";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Tạo chi nhánh thành công");
        return "redirect:/branches";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable String id, Model model) {
        Branch branch = branchService.getBranchById(id);

        model.addAttribute("branchId", branch.getId());
        model.addAttribute("branchRequest", branchMapper.toBranchRequest(branch));

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Chi nhánh", "/branches"),
                new BreadcrumbItem("Sửa", null)
        ));

        return "branches/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable String id,
            @Valid @ModelAttribute("branchRequest") BranchRequest branchRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("branchId", id);
            addEditBreadcrumb(model);
            return "branches/edit";
        }

        boolean success = branchService.updateBranch(id, branchRequest, bindingResult);

        if (!success) {
            model.addAttribute("branchId", id);
            addEditBreadcrumb(model);
            return "branches/edit";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật chi nhánh thành công");
        return "redirect:/branches";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(
            @PathVariable String id,
            RedirectAttributes redirectAttributes
    ) {
        branchService.toggleActive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái chi nhánh thành công");
        return "redirect:/branches";
    }

    private void addCreateBreadcrumb(Model model) {
        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Chi nhánh", "/branches"),
                new BreadcrumbItem("Thêm mới", null)
        ));
    }

    private void addEditBreadcrumb(Model model) {
        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Chi nhánh", "/branches"),
                new BreadcrumbItem("Sửa", null)
        ));
    }
}