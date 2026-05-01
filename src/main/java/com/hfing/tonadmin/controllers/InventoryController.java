package com.hfing.tonadmin.controllers;

import com.hfing.tonadmin.dto.BreadcrumbItem;
import com.hfing.tonadmin.dto.request.InventoryImportItemRequest;
import com.hfing.tonadmin.dto.request.InventoryImportRequest;
import com.hfing.tonadmin.dto.response.StockTransactionSummaryProjection;
import com.hfing.tonadmin.entities.Inventory;
import com.hfing.tonadmin.repositories.BranchRepository;
import com.hfing.tonadmin.repositories.ProductRepository;
import com.hfing.tonadmin.services.InventoryService;
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
@RequestMapping("/inventories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {

    private final InventoryService inventoryService;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public String index(
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Inventory> inventoryPage = inventoryService.getInventoryPage(branchId, productId, pageable);

        model.addAttribute("inventoryPage", inventoryPage);
        model.addAttribute("inventories", inventoryPage.getContent());

        model.addAttribute("branches", branchRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("products", productRepository.findByActiveTrueOrderByNameAsc());

        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("selectedProductId", productId);

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Tồn kho", null)
        ));

        return "inventories/index";
    }

    @GetMapping("/import")
    public String importPage(Model model) {
        model.addAttribute("inventoryImportRequest", createEmptyImportRequest());
        addImportAttributes(model);

        return "inventories/import";
    }

    @PostMapping("/import")
    public String importInventory(
            @Valid @ModelAttribute("inventoryImportRequest") InventoryImportRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addImportAttributes(model);
            return "inventories/import";
        }

        boolean success = inventoryService.importInventory(request, bindingResult);

        if (!success) {
            addImportAttributes(model);
            return "inventories/import";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Nhập kho thành công");
        return "redirect:/inventories";
    }

    @GetMapping("/transactions")
    public String transactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<StockTransactionSummaryProjection> transactionPage =
                inventoryService.getTransactionSummaryPage(pageable);

        model.addAttribute("transactionPage", transactionPage);
        model.addAttribute("transactionSummaries", transactionPage.getContent());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Tồn kho", "/inventories"),
                new BreadcrumbItem("Lịch sử kho", null)
        ));

        return "inventories/transactions";
    }

    private void addImportAttributes(Model model) {
        model.addAttribute("branches", branchRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("products", productRepository.findByActiveTrueOrderByNameAsc());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Tồn kho", "/inventories"),
                new BreadcrumbItem("Nhập kho", null)
        ));
    }

    private InventoryImportRequest createEmptyImportRequest() {
        return InventoryImportRequest.builder()
                .branchId("")
                .note("")
                .items(List.of(
                        InventoryImportItemRequest.builder()
                                .productId("")
                                .quantity(BigDecimal.ONE)
                                .build()
                ))
                .build();
    }
}