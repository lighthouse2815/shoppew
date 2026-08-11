package com.shoppew.checkout.dto;

import com.shoppew.payment.entity.PaymentProviderType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record CheckoutRequest(
        @NotEmpty @Size(max = 100) Set<UUID> cartItemIds,
        @NotNull UUID addressId,
        @NotNull PaymentProviderType paymentProvider,
        @Size(max = 80) String shippingMethodCode,
        @Size(max = 500) String customerNote,
        @Size(max = 5) Set<@Pattern(regexp = "^[A-Za-z0-9_-]{3,40}$") String> voucherCodes) {}
