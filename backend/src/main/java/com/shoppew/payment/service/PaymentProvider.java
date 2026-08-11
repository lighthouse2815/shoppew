package com.shoppew.payment.service;

import com.shoppew.payment.entity.PaymentProviderType;
import java.math.BigDecimal;

public interface PaymentProvider {
    PaymentProviderType type();
    PaymentInitiation initiate(String checkoutNumber, BigDecimal amount, String currency);

    record PaymentInitiation(String providerReference, String action, boolean immediatelyAccepted) {}
}
