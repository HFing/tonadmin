package com.hfing.tonadmin.controllers;

import com.hfing.tonadmin.common.PaymentMethod;
import com.hfing.tonadmin.dto.BreadcrumbItem;
import com.hfing.tonadmin.dto.request.CancelSalesOrderRequest;
import com.hfing.tonadmin.dto.request.PaymentUpdateRequest;
import com.hfing.tonadmin.dto.request.SalesOrderItemRequest;
import com.hfing.tonadmin.dto.request.SalesOrderRequest;
import com.hfing.tonadmin.entities.SalesOrder;
import com.hfing.tonadmin.repositories.BranchRepository;
import com.hfing.tonadmin.repositories.ProductRepository;
import com.hfing.tonadmin.services.CurrentUserService;
import com.hfing.tonadmin.services.SalesOrderService;
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
@RequestMapping("/sales-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_STAFF')")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<SalesOrder> salesOrderPage = salesOrderService.getSalesOrders(pageable);

        model.addAttribute("salesOrderPage", salesOrderPage);
        model.addAttribute("salesOrders", salesOrderPage.getContent());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Đơn bán hàng", null)
        ));

        return "sales-orders/index";
    }

    @GetMapping("/create")
    public String createPage(Model model) {
        model.addAttribute("salesOrderRequest", createEmptyRequest());
        addFormAttributes(model);

        return "sales-orders/create";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("salesOrderRequest") SalesOrderRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addFormAttributes(model);
            return "sales-orders/create";
        }

        SalesOrder salesOrder = salesOrderService.createSalesOrder(request, bindingResult);

        if (salesOrder == null) {
            addFormAttributes(model);
            return "sales-orders/create";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Tạo đơn bán hàng thành công. Vui lòng cập nhật thanh toán."
        );

        return "redirect:/sales-orders/" + salesOrder.getId() + "?payment=true";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        addDetailAttributes(id, model);
        return "sales-orders/detail";
    }

    @PostMapping("/{id}/payment")
    public String updatePayment(
            @PathVariable String id,
            @Valid @ModelAttribute("paymentUpdateRequest") PaymentUpdateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            addDetailAttributes(id, model);
            return "sales-orders/detail";
        }

        boolean success = salesOrderService.updatePayment(id, request, bindingResult);

        if (!success) {
            addDetailAttributes(id, model);
            return "sales-orders/detail";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thanh toán thành công");
        return "redirect:/sales-orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable String id,
            @ModelAttribute("cancelSalesOrderRequest") CancelSalesOrderRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        boolean success = salesOrderService.cancelSalesOrder(id, request, bindingResult);

        if (!success) {
            addDetailAttributes(id, model);
            return "sales-orders/detail";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Hủy đơn bán hàng thành công. Tồn kho đã được hoàn lại."
        );

        return "redirect:/sales-orders/" + id;
    }

    private SalesOrderRequest createEmptyRequest() {
        String branchId = "";

        if (!currentUserService.isAdmin()) {
            branchId = currentUserService.getCurrentUserBranch().getId();
        }

        return SalesOrderRequest.builder()
                .branchId(branchId)
                .customerName("")
                .customerPhone("")
                .discountAmount(BigDecimal.ZERO)
                .note("")
                .items(List.of(
                        SalesOrderItemRequest.builder()
                                .productId("")
                                .quantity(BigDecimal.ONE)
                                .build()
                ))
                .build();
    }

    private void addFormAttributes(Model model) {
        boolean isAdmin = currentUserService.isAdmin();

        model.addAttribute("isAdmin", isAdmin);

        if (isAdmin) {
            model.addAttribute("branches", branchRepository.findByActiveTrueOrderByNameAsc());
            model.addAttribute("staffBranch", null);
        } else {
            model.addAttribute("branches", List.of());
            model.addAttribute("staffBranch", currentUserService.getCurrentUserBranch());
        }

        model.addAttribute("products", productRepository.findByActiveTrueOrderByNameAsc());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Đơn bán hàng", "/sales-orders"),
                new BreadcrumbItem("Tạo đơn", null)
        ));
    }

    private void addDetailAttributes(String id, Model model) {
        SalesOrder salesOrder = salesOrderService.getSalesOrderById(id);

        model.addAttribute("salesOrder", salesOrder);
        model.addAttribute("items", salesOrderService.getSalesOrderItems(id));
        model.addAttribute("payments", salesOrderService.getSalesPayments(id));
        model.addAttribute("paymentMethods", PaymentMethod.values());

        model.addAttribute("paymentUpdateRequest", PaymentUpdateRequest.builder()
                .paymentMethod(PaymentMethod.CASH)
                .amount(salesOrder.getRemainingAmount() == null
                        ? BigDecimal.ZERO
                        : salesOrder.getRemainingAmount())
                .build());

        model.addAttribute("cancelSalesOrderRequest", CancelSalesOrderRequest.builder()
                .reason("")
                .build());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Đơn bán hàng", "/sales-orders"),
                new BreadcrumbItem(salesOrder.getOrderCode(), null)
        ));
    }
}