package com.hfing.tonadmin.specifications;

import com.hfing.tonadmin.dto.request.SalesOrderSearchRequest;
import com.hfing.tonadmin.entities.SalesOrder;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class SalesOrderSpecifications {

    private SalesOrderSpecifications() {
    }

    public static Specification<SalesOrder> bySearch(SalesOrderSearchRequest search) {
        return Specification.allOf(
                keyword(search.keyword()),
                branchId(search.branchId()),
                status(search),
                paymentStatus(search),
                debtOnly(search),
                createdFrom(search.fromDate()),
                createdBefore(search.toDate())
        );
    }

    private static Specification<SalesOrder> keyword(String keyword) {
        String normalized = normalize(keyword);

        return (root, query, cb) -> {
            if (normalized == null) {
                return cb.conjunction();
            }

            String pattern = "%" + normalized + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("orderCode")), pattern),
                    cb.like(cb.lower(root.get("customerName")), pattern),
                    cb.like(cb.lower(root.get("customerPhone")), pattern),
                    cb.like(cb.lower(root.get("branch").get("name")), pattern)
            );
        };
    }

    private static Specification<SalesOrder> branchId(String branchId) {
        return (root, query, cb) -> {
            if (branchId == null || branchId.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(root.get("branch").get("id"), branchId);
        };
    }

    private static Specification<SalesOrder> status(SalesOrderSearchRequest search) {
        return (root, query, cb) -> search.status() == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), search.status());
    }

    private static Specification<SalesOrder> paymentStatus(SalesOrderSearchRequest search) {
        return (root, query, cb) -> search.paymentStatus() == null
                ? cb.conjunction()
                : cb.equal(root.get("paymentStatus"), search.paymentStatus());
    }

    private static Specification<SalesOrder> debtOnly(SalesOrderSearchRequest search) {
        return (root, query, cb) -> !Boolean.TRUE.equals(search.debtOnly())
                ? cb.conjunction()
                : cb.and(
                        cb.notEqual(root.get("status"), com.hfing.tonadmin.common.SalesOrderStatus.CANCELLED),
                        cb.greaterThan(root.get("remainingAmount"), java.math.BigDecimal.ZERO)
                );
    }

    private static Specification<SalesOrder> createdFrom(LocalDate fromDate) {
        return (root, query, cb) -> fromDate == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay(fromDate));
    }

    private static Specification<SalesOrder> createdBefore(LocalDate toDate) {
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
