package com.hfing.tonadmin.dto.request;

public record ProductSearchRequest(
        String keyword,
        String categoryId,
        Boolean active
) {
}
