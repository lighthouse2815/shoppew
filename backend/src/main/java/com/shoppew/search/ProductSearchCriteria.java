package com.shoppew.search;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSearchCriteria(
        String query,
        UUID shopId,
        UUID categoryId,
        UUID brandId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minRating,
        SearchSort sort,
        int page,
        int size) {}
