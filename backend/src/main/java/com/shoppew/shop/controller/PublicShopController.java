package com.shoppew.shop.controller;

import com.shoppew.common.api.ApiResponse;
import com.shoppew.shop.dto.ShopResponse;
import com.shoppew.shop.service.ShopService;
import java.time.Clock;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/shops")
public class PublicShopController {

    private final ShopService shopService;
    private final Clock clock;

    public PublicShopController(ShopService shopService, Clock clock) {
        this.shopService = shopService;
        this.clock = clock;
    }

    @GetMapping("/{slug}")
    ApiResponse<ShopResponse> get(@PathVariable String slug) {
        return ApiResponse.success(shopService.publicShop(slug), clock);
    }
}
