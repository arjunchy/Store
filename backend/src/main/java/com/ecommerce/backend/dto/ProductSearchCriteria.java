package com.ecommerce.backend.dto;

import java.math.BigDecimal;

public record ProductSearchCriteria(
        String name,
        String categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean isNewArrival,
        Float minRating,
        String sortBy,
        String sortDirection
) {
}
