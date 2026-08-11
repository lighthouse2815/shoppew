package com.shoppew.shipping.repository;

import com.shoppew.shipping.entity.ShipmentTrackingEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTrackingEntity, UUID> {
    List<ShipmentTrackingEntity> findAllByShipment_IdOrderByOccurredAtAsc(UUID shipmentId);
}
