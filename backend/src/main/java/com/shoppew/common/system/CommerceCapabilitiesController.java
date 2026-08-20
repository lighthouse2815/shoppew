package com.shoppew.common.system;

import com.shoppew.common.api.ApiResponse;
import com.shoppew.payment.service.PaymentProviderRegistry;
import com.shoppew.shipping.service.ShippingProviderRegistry;
import java.time.Clock;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/commerce-capabilities")
public class CommerceCapabilitiesController {

    private final PaymentProviderRegistry paymentProviders;
    private final ShippingProviderRegistry shippingProviders;
    private final Clock clock;

    public CommerceCapabilitiesController(
            PaymentProviderRegistry paymentProviders,
            ShippingProviderRegistry shippingProviders,
            Clock clock) {
        this.paymentProviders = paymentProviders;
        this.shippingProviders = shippingProviders;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<CommerceCapabilitiesResponse> capabilities() {
        return ApiResponse.success(new CommerceCapabilitiesResponse(
                paymentProviders.availableTypes(),
                shippingProviders.availableMethodCodes()), clock);
    }
}
