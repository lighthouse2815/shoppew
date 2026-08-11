package com.shoppew.shipping.repository;

import com.shoppew.shipping.entity.ShippingMethodEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingMethodRepository extends JpaRepository<ShippingMethodEntity, UUID> {
    Optional<ShippingMethodEntity> findByProviderAndCodeAndActiveTrue(String provider, String code);
}
