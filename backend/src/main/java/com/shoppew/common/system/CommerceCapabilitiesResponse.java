package com.shoppew.common.system;

import com.shoppew.payment.entity.PaymentProviderType;
import java.util.List;

public record CommerceCapabilitiesResponse(
        List<PaymentProviderType> availablePaymentProviders,
        List<String> availableShippingMethods) {}
