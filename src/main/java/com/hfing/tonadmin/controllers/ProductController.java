package com.hfing.tonadmin.controllers;

import com.hfing.tonadmin.common.UnitType;
import com.hfing.tonadmin.dto.BreadcrumbItem;
import com.hfing.tonadmin.dto.request.ProductRequest;
import com.hfing.tonadmin.entities.Product;
import com.hfing.tonadmin.mappers.ProductMapper;
import com.hfing.tonadmin.repositories.ProductCategoryRepository;
import com.hfing.tonadmin.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final ProductCategoryRepository productCategoryRepository;

    @GetMapping
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Product> productPage = productService.getProducts(pageable);

        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Sản phẩm", null)
        ));

        return "products/index";
    }

    @GetMapping("/create")
    public String createPage(Model model) {
        model.addAttribute("productRequest", new ProductRequest(
                "",
                "",
                "",
                "",
                null,
                null,
                null,
                null,
                "",
                "",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ));

        addFormAttributes(model, "Thêm mới");

        return "products/create";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("productRequest") ProductRequest productRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addFormAttributes(model, "Thêm mới");
            return "products/create";
        }

        boolean success = productService.createProduct(productRequest, bindingResult);

        if (!success) {
            addFormAttributes(model, "Thêm mới");
            return "products/create";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Tạo sản phẩm thành công");
        return "redirect:/products";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable String id, Model model) {
        Product product = productService.getProductById(id);

        model.addAttribute("productId", product.getId());
        model.addAttribute("productRequest", productMapper.toProductRequest(product));

        addFormAttributes(model, "Sửa");

        return "products/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable String id,
            @Valid @ModelAttribute("productRequest") ProductRequest productRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", id);
            addFormAttributes(model, "Sửa");
            return "products/edit";
        }

        boolean success = productService.updateProduct(id, productRequest, bindingResult);

        if (!success) {
            model.addAttribute("productId", id);
            addFormAttributes(model, "Sửa");
            return "products/edit";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công");
        return "redirect:/products";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(
            @PathVariable String id,
            RedirectAttributes redirectAttributes
    ) {
        productService.toggleActive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái sản phẩm thành công");
        return "redirect:/products";
    }

    private void addFormAttributes(Model model, String currentPage) {
        model.addAttribute("categories", productCategoryRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("units", UnitType.values());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Sản phẩm", "/products"),
                new BreadcrumbItem(currentPage, null)
        ));
    }
}