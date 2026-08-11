package com.shoppew.finance.repository;

import com.shoppew.finance.entity.SellerTransactionEntity;
import com.shoppew.finance.entity.SellerTransactionType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerTransactionRepository extends JpaRepository<SellerTransactionEntity, UUID> {
    boolean existsByShop_IdAndTransactionTypeAndReferenceKey(UUID shopId, SellerTransactionType type, String referenceKey);
    Page<SellerTransactionEntity> findAllByShop_Id(UUID shopId, Pageable pageable);
}
