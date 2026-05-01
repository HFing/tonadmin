package com.hfing.tonadmin.controllers;

import com.hfing.tonadmin.common.RoleType;
import com.hfing.tonadmin.common.UserStatus;
import com.hfing.tonadmin.dto.BreadcrumbItem;
import com.hfing.tonadmin.dto.request.CreateUserRequest;
import com.hfing.tonadmin.dto.request.UpdateUserRequest;
import com.hfing.tonadmin.entities.User;
import com.hfing.tonadmin.mappers.UserMapper;
import com.hfing.tonadmin.repositories.BranchRepository;
import com.hfing.tonadmin.services.UserService;
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
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final BranchRepository branchRepository;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("users", userService.getAllUsers());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Người dùng", null)
        ));

        return "users/index";
    }

    @GetMapping("/create")
    public String createPage(Model model) {
        model.addAttribute("userRequest", new CreateUserRequest(
                "", "", "", "", "", UserStatus.ACTIVE, RoleType.BRANCH_STAFF, ""
        ));

        addFormAttributes(model, "Thêm mới");

        return "users/create";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("userRequest") CreateUserRequest createUserRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addFormAttributes(model, "Thêm mới");
            return "users/create";
        }

        boolean success = userService.createUser(createUserRequest, bindingResult);

        if (!success) {
            addFormAttributes(model, "Thêm mới");
            return "users/create";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Tạo người dùng thành công");
        return "redirect:/users";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable String id, Model model) {
        User user = userService.getUserById(id);

        UpdateUserRequest updateUserRequest = userMapper.toUpdateUserRequest(user);

        RoleType roleType = user.getUserHasRoles()
                .stream()
                .findFirst()
                .map(userHasRole -> userHasRole.getRole().getRoleType())
                .orElse(RoleType.BRANCH_STAFF);

        String branchId = user.getBranch() != null ? user.getBranch().getId() : "";

        updateUserRequest = new UpdateUserRequest(
                updateUserRequest.email(),
                "",
                updateUserRequest.firstName(),
                updateUserRequest.lastName(),
                updateUserRequest.phone(),
                updateUserRequest.userStatus(),
                roleType,
                branchId
        );

        model.addAttribute("userId", user.getId());
        model.addAttribute("userRequest", updateUserRequest);

        addFormAttributes(model, "Sửa");

        return "users/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable String id,
            @Valid @ModelAttribute("userRequest") UpdateUserRequest updateUserRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            addFormAttributes(model, "Sửa");
            return "users/edit";
        }

        boolean success = userService.updateUser(id, updateUserRequest, bindingResult);

        if (!success) {
            model.addAttribute("userId", id);
            addFormAttributes(model, "Sửa");
            return "users/edit";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật người dùng thành công");
        return "redirect:/users";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(
            @PathVariable String id,
            RedirectAttributes redirectAttributes
    ) {
        userService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái người dùng thành công");
        return "redirect:/users";
    }

    private void addFormAttributes(Model model, String currentPage) {
        model.addAttribute("branches", branchRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("roles", RoleType.values());
        model.addAttribute("statuses", UserStatus.values());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Người dùng", "/users"),
                new BreadcrumbItem(currentPage, null)
        ));
    }
}