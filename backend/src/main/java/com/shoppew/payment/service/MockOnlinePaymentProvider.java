package com.shoppew.payment.service;

import com.shoppew.payment.entity.PaymentProviderType;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.payment.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockOnlinePaymentProvider implements PaymentProvider {
    @Override public PaymentProviderType type() { return PaymentProviderType.MOCK_ONLINE; }
    @Override
    public PaymentInitiation initiate(String checkoutNumber, BigDecimal amount, String currency) {
        String reference = "MOCK-" + UUID.randomUUID();
        return new PaymentInitiation(reference, "MOCK_WEBHOOK_REQUIRED", false);
    }
}
