package com.shoppew.wishlist.dto;

import com.shoppew.product.dto.ProductSummaryResponse;
import java.time.Instant;
import java.util.UUID;

public record WishlistResponse(UUID id, ProductSummaryResponse product, Instant createdAt) {}
