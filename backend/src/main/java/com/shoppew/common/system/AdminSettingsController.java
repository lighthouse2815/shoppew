package com.shoppew.common.system;

import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.config.AppProperties;
import com.shoppew.media.ImageUploadValidator;
import com.shoppew.payment.service.PaymentProviderRegistry;
import com.shoppew.shipping.service.ShippingProviderRegistry;
import java.time.Clock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminSettingsController {

    private final AppProperties properties;
    private final PaymentProviderRegistry paymentProviders;
    private final ShippingProviderRegistry shippingProviders;
    private final Clock clock;

    public AdminSettingsController(
            AppProperties properties,
            PaymentProviderRegistry paymentProviders,
            ShippingProviderRegistry shippingProviders,
            Clock clock) {
        this.properties = properties;
        this.paymentProviders = paymentProviders;
        this.shippingProviders = shippingProviders;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<AdminSettingsResponse> settings() {
        AdminSettingsResponse response = new AdminSettingsResponse(
                properties.locale(), properties.currency(), properties.timeZone(),
                paymentProviders.availableTypes().stream().map(Enum::name).toList(),
                shippingProviders.availableMethodCodes(), "S3_COMPATIBLE",
                ImageUploadValidator.MAX_IMAGE_BYTES);
        return ApiResponse.success(response, clock);
    }
}
