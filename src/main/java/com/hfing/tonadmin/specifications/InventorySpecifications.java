package com.hfing.tonadmin.specifications;

import com.hfing.tonadmin.common.StockStatus;
import com.hfing.tonadmin.dto.request.InventorySearchRequest;
import com.hfing.tonadmin.entities.Inventory;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class InventorySpecifications {

    private InventorySpecifications() {
    }

    public static Specification<Inventory> bySearch(InventorySearchRequest search) {
        return Specification.allOf(
                keyword(search.keyword()),
                branchId(search.branchId()),
                productId(search.productId()),
                stockStatus(search.stockStatus())
        );
    }

    private static Specification<Inventory> keyword(String keyword) {
        String normalized = normalize(keyword);

        return (root, query, cb) -> {
            if (normalized == null) {
                return cb.conjunction();
            }

            String pattern = "%" + normalized + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("branch").get("name")), pattern),
                    cb.like(cb.lower(root.get("product").get("code")), pattern),
                    cb.like(cb.lower(root.get("product").get("name")), pattern),
                    cb.like(cb.lower(root.get("product").get("category").get("name")), pattern)
            );
        };
    }

    private static Specification<Inventory> branchId(String branchId) {
        return (root, query, cb) -> branchId == null || branchId.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("branch").get("id"), branchId);
    }

    private static Specification<Inventory> productId(String productId) {
        return (root, query, cb) -> productId == null || productId.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("product").get("id"), productId);
    }

    private static Specification<Inventory> stockStatus(StockStatus stockStatus) {
        return (root, query, cb) -> {
            if (stockStatus == null) {
                return cb.conjunction();
            }

            if (stockStatus == StockStatus.OUT_OF_STOCK) {
                return cb.lessThanOrEqualTo(root.get("quantity"), BigDecimal.ZERO);
            }

            if (stockStatus == StockStatus.LOW_STOCK) {
                return cb.and(
                        cb.greaterThan(root.get("quantity"), BigDecimal.ZERO),
                        cb.lessThanOrEqualTo(root.get("quantity"), root.get("product").get("minStock"))
                );
            }

            return cb.greaterThan(root.get("quantity"), root.get("product").get("minStock"));
        };
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase();
    }
}
