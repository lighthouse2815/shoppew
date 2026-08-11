package com.shoppew.search;

import com.shoppew.common.api.PageResponse;
import com.shoppew.product.dto.ProductSummaryResponse;
import java.util.List;

public interface SearchService {

    PageResponse<ProductSummaryResponse> search(ProductSearchCriteria criteria);

    List<String> suggest(String query, int size);
}
