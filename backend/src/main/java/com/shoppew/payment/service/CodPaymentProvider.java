package com.shoppew.payment.service;

import com.shoppew.payment.entity.PaymentProviderType;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CodPaymentProvider implements PaymentProvider {
    @Override public PaymentProviderType type() { return PaymentProviderType.COD; }
    @Override
    public PaymentInitiation initiate(String checkoutNumber, BigDecimal amount, String currency) {
        return new PaymentInitiation("COD-" + checkoutNumber, "PAY_ON_DELIVERY", true);
    }
}
