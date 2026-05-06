package com.hfing.tonadmin.specifications;

import com.hfing.tonadmin.dto.request.StockTransferSearchRequest;
import com.hfing.tonadmin.entities.StockTransfer;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class StockTransferSpecifications {

    private StockTransferSpecifications() {
    }

    public static Specification<StockTransfer> bySearch(StockTransferSearchRequest search) {
        return Specification.allOf(
                keyword(search.keyword()),
                sourceBranchId(search.sourceBranchId()),
                targetBranchId(search.targetBranchId()),
                createdFrom(search.fromDate()),
                createdBefore(search.toDate())
        );
    }

    private static Specification<StockTransfer> keyword(String keyword) {
        String normalized = normalize(keyword);

        return (root, query, cb) -> {
            if (normalized == null) {
                return cb.conjunction();
            }

            String pattern = "%" + normalized + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("transferCode")), pattern),
                    cb.like(cb.lower(root.get("note")), pattern),
                    cb.like(cb.lower(root.get("sourceBranch").get("name")), pattern),
                    cb.like(cb.lower(root.get("targetBranch").get("name")), pattern)
            );
        };
    }

    private static Specification<StockTransfer> sourceBranchId(String branchId) {
        return (root, query, cb) -> branchId == null || branchId.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("sourceBranch").get("id"), branchId);
    }

    private static Specification<StockTransfer> targetBranchId(String branchId) {
        return (root, query, cb) -> branchId == null || branchId.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("targetBranch").get("id"), branchId);
    }

    private static Specification<StockTransfer> createdFrom(LocalDate fromDate) {
        return (root, query, cb) -> fromDate == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay(fromDate));
    }

    private static Specification<StockTransfer> createdBefore(LocalDate toDate) {
        return (root, query, cb) -> toDate == null
                ? cb.conjunction()
                : cb.lessThan(root.get("createdAt"), startOfDay(toDate.plusDays(1)));
    }

    private static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase();
    }
}
