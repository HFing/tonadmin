package com.hfing.tonadmin.controllers;

import com.hfing.tonadmin.dto.BreadcrumbItem;
import com.hfing.tonadmin.dto.request.StockTransferItemRequest;
import com.hfing.tonadmin.dto.request.StockTransferRequest;
import com.hfing.tonadmin.dto.request.StockTransferSearchRequest;
import com.hfing.tonadmin.entities.StockTransfer;
import com.hfing.tonadmin.repositories.BranchRepository;
import com.hfing.tonadmin.repositories.ProductRepository;
import com.hfing.tonadmin.services.StockTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/stock-transfers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StockTransferController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final StockTransferService stockTransferService;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public String index(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sourceBranchId,
            @RequestParam(required = false) String targetBranchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        StockTransferSearchRequest search = new StockTransferSearchRequest(
                keyword,
                sourceBranchId,
                targetBranchId,
                fromDate,
                toDate
        );
        Page<StockTransfer> transferPage = stockTransferService.getTransfers(search, pageable);

        model.addAttribute("transferPage", transferPage);
        model.addAttribute("transfers", transferPage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("branches", branchRepository.findByActiveTrueOrderByNameAsc());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Chuyển kho", null)
        ));

        return "stock-transfers/index";
    }

    @GetMapping("/create")
    public String createPage(Model model) {
        model.addAttribute("stockTransferRequest", createEmptyRequest());
        addFormAttributes(model);

        return "stock-transfers/create";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("stockTransferRequest") StockTransferRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addFormAttributes(model);
            return "stock-transfers/create";
        }

        StockTransfer stockTransfer = stockTransferService.createTransfer(request, bindingResult);

        if (stockTransfer == null) {
            addFormAttributes(model);
            return "stock-transfers/create";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Chuyển kho thành công");
        return "redirect:/stock-transfers/" + stockTransfer.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        StockTransfer stockTransfer = stockTransferService.getTransferById(id);

        model.addAttribute("stockTransfer", stockTransfer);
        model.addAttribute("items", stockTransferService.getTransferItems(id));

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Chuyển kho", "/stock-transfers"),
                new BreadcrumbItem(stockTransfer.getTransferCode(), null)
        ));

        return "stock-transfers/detail";
    }

    @GetMapping("/{id}/print")
    public String print(@PathVariable String id, Model model) {
        StockTransfer stockTransfer = stockTransferService.getTransferById(id);

        model.addAttribute("stockTransfer", stockTransfer);
        model.addAttribute("items", stockTransferService.getTransferItems(id));
        model.addAttribute("stockTransferCreatedAtText", formatInstant(stockTransfer.getCreatedAt()));

        return "stock-transfers/print";
    }

    private String formatInstant(Instant instant) {
        return instant == null ? "—" : DATE_TIME_FORMATTER.format(instant);
    }

    private StockTransferRequest createEmptyRequest() {
        return StockTransferRequest.builder()
                .sourceBranchId("")
                .targetBranchId("")
                .note("")
                .items(List.of(
                        StockTransferItemRequest.builder()
                                .productId("")
                                .quantity(BigDecimal.ONE)
                                .build()
                ))
                .build();
    }

    private void addFormAttributes(Model model) {
        model.addAttribute("branches", branchRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("products", productRepository.findByActiveTrueOrderByNameAsc());

        model.addAttribute("breadcrumbs", List.of(
                new BreadcrumbItem("Dashboard", "/dashboard"),
                new BreadcrumbItem("Chuyển kho", "/stock-transfers"),
                new BreadcrumbItem("Tạo phiếu chuyển", null)
        ));
    }
}
