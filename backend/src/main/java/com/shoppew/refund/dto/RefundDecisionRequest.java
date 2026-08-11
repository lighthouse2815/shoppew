package com.shoppew.refund.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RefundDecisionRequest(@NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal approvedAmount,
        @Size(max = 1000) String note) {}
