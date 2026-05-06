package com.hfing.tonadmin.specifications;

import com.hfing.tonadmin.dto.request.ProductSearchRequest;
import com.hfing.tonadmin.entities.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> bySearch(ProductSearchRequest search) {
        return Specification.allOf(
                keyword(search.keyword()),
                categoryId(search.categoryId()),
                active(search.active())
        );
    }

    private static Specification<Product> keyword(String keyword) {
        String normalized = normalize(keyword);

        return (root, query, cb) -> {
            if (normalized == null) {
                return cb.conjunction();
            }

            String pattern = "%" + normalized + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("color")), pattern),
                    cb.like(cb.lower(root.get("material")), pattern),
                    cb.like(cb.lower(root.get("category").get("name")), pattern)
            );
        };
    }

    private static Specification<Product> categoryId(String categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null || categoryId.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }

    private static Specification<Product> active(Boolean active) {
        return (root, query, cb) -> active == null
                ? cb.conjunction()
                : cb.equal(root.get("active"), active);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase();
    }
}
