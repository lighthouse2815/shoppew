package com.shoppew.inventory.repository;

import com.shoppew.inventory.entity.InventoryTransactionEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionEntity, UUID> {
    Page<InventoryTransactionEntity> findAllByVariant_Id(UUID variantId, Pageable pageable);
}
