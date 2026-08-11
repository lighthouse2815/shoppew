package com.shoppew.shipping.repository;

import com.shoppew.shipping.entity.ShipmentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<ShipmentEntity, UUID> {
    Optional<ShipmentEntity> findByOrder_Id(UUID orderId);
}
