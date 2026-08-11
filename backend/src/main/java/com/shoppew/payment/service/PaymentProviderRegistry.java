package com.shoppew.payment.service;

import com.shoppew.common.exception.ApiException;
import com.shoppew.payment.entity.PaymentProviderType;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderRegistry {
    private final List<PaymentProvider> providers;
    public PaymentProviderRegistry(List<PaymentProvider> providers) { this.providers = List.copyOf(providers); }

    public PaymentProvider require(PaymentProviderType type) {
        if (type != PaymentProviderType.COD && type != PaymentProviderType.MOCK_ONLINE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_PROVIDER_NOT_AVAILABLE",
                    "Phương thức thanh toán chưa khả dụng trong môi trường này");
        }
        return providers.stream().filter(provider -> provider.type() == type).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_PROVIDER_NOT_AVAILABLE",
                        "Phương thức thanh toán không khả dụng"));
    }

    public List<PaymentProviderType> availableTypes() {
        return providers.stream().map(PaymentProvider::type).sorted().toList();
    }
}
