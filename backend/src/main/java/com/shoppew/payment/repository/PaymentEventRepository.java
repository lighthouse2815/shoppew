package com.shoppew.payment.repository;

import com.shoppew.payment.entity.PaymentEventEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEventEntity, UUID> {
    Optional<PaymentEventEntity> findByProviderAndProviderEventId(String provider, String providerEventId);
}
