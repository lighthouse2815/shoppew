package com.shoppew.shipping.service;

import com.shoppew.common.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ShippingProviderRegistry {
    private final List<ShippingProvider> providers;
    public ShippingProviderRegistry(List<ShippingProvider> providers) { this.providers = List.copyOf(providers); }

    public ShippingProvider require(String methodCode) {
        return providers.stream().filter(provider -> provider.methodCode().equals(methodCode)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "SHIPPING_METHOD_NOT_AVAILABLE",
                        "Phương thức vận chuyển không khả dụng"));
    }

    public List<String> availableMethodCodes() {
        return providers.stream().map(ShippingProvider::methodCode).sorted().toList();
    }
}
