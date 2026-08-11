package com.shoppew.promotion.dto;

import com.shoppew.promotion.entity.PromotionType;
import com.shoppew.voucher.entity.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PromotionRequest(
        @NotBlank @Size(max = 180) String name,
        @NotNull PromotionType promotionType,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0.01") BigDecimal discountValue,
        @DecimalMin("0.01") BigDecimal maxDiscount,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotEmpty @Size(max = 500) List<@Valid PromotionTargetRequest> targets) {}
