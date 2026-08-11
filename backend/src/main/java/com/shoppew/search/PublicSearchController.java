package com.shoppew.search;

import com.shoppew.common.api.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public/search")
public class PublicSearchController {

    private final SearchService searchService;
    private final Clock clock;

    public PublicSearchController(SearchService searchService, Clock clock) {
        this.searchService = searchService;
        this.clock = clock;
    }

    @GetMapping("/suggestions")
    ApiResponse<List<String>> suggestions(
            @RequestParam @Size(min = 2, max = 200) String q,
            @RequestParam(defaultValue = "8") @Min(1) @Max(10) int size) {
        return ApiResponse.success(searchService.suggest(q, size), clock);
    }
}
