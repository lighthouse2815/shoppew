package com.shoppew.recommendation;

import com.shoppew.product.dto.ProductSummaryResponse;
import java.util.List;
import java.util.UUID;

public interface RecommendationService {

    List<ProductSummaryResponse> popular(int size);

    List<ProductSummaryResponse> trending(int size);

    List<ProductSummaryResponse> related(UUID productId, int size);

    List<ProductSummaryResponse> sameShop(UUID shopId, UUID excludeProductId, int size);

    void recordView(UUID userId, UUID productId);

    List<ProductSummaryResponse> recentlyViewed(UUID userId, int size);
}
