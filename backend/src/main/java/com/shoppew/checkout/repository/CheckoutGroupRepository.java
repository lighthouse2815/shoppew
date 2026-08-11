package com.shoppew.checkout.repository;

import com.shoppew.checkout.entity.CheckoutGroupEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutGroupRepository extends JpaRepository<CheckoutGroupEntity, UUID> {
    Optional<CheckoutGroupEntity> findByUser_IdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
