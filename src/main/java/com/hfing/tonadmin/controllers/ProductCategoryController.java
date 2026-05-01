package com.hfing.tonadmin.controllers;

import com.hfing.tonadmin.dto.BreadcrumbItem;
import com.hfing.tonadmin.dto.request.ProductCategoryRequest;
import com.hfing.tonadmin.entities.ProductCategory;
import com.hfing.tonadmin.mappers.ProductCategoryMapper;
import com.hfing.tonadmin.services.ProductCategoryService;
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
@RequestMapping("/product-categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;
    private final ProductCategoryMapper productCategoryMapper;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("categories", productCategoryService.getAllCategories());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Danh mục sản phẩm", null)
        ));

        return "product-categories/index";
    }

    @GetMapping("/create")
    public String createPage(Model model) {
        model.addAttribute("categoryRequest", new ProductCategoryRequest("", ""));

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Danh mục sản phẩm", "/product-categories"),
                new BreadcrumbItem("Thêm mới", null)
        ));

        return "product-categories/create";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("categoryRequest") ProductCategoryRequest categoryRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addCreateBreadcrumb(model);
            return "product-categories/create";
        }

        boolean success = productCategoryService.createCategory(categoryRequest, bindingResult);

        if (!success) {
            addCreateBreadcrumb(model);
            return "product-categories/create";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Tạo danh mục sản phẩm thành công");
        return "redirect:/product-categories";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable String id, Model model) {
        ProductCategory category = productCategoryService.getCategoryById(id);

        model.addAttribute("categoryId", category.getId());
        model.addAttribute("categoryRequest", productCategoryMapper.toProductCategoryRequest(category));

        addEditBreadcrumb(model);

        return "product-categories/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable String id,
            @Valid @ModelAttribute("categoryRequest") ProductCategoryRequest categoryRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryId", id);
            addEditBreadcrumb(model);
            return "product-categories/edit";
        }

        boolean success = productCategoryService.updateCategory(id, categoryRequest, bindingResult);

        if (!success) {
            model.addAttribute("categoryId", id);
            addEditBreadcrumb(model);
            return "product-categories/edit";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục sản phẩm thành công");
        return "redirect:/product-categories";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(
            @PathVariable String id,
            RedirectAttributes redirectAttributes
    ) {
        productCategoryService.toggleActive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái danh mục thành công");
        return "redirect:/product-categories";
    }

    private void addCreateBreadcrumb(Model model) {
        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Danh mục sản phẩm", "/product-categories"),
                new BreadcrumbItem("Thêm mới", null)
        ));
    }

    private void addEditBreadcrumb(Model model) {
        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Danh mục sản phẩm", "/product-categories"),
                new BreadcrumbItem("Sửa", null)
        ));
    }
}