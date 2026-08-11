package com.shoppew.finance.repository;

import com.shoppew.finance.entity.SellerBalanceEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellerBalanceRepository extends JpaRepository<SellerBalanceEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select balance from SellerBalanceEntity balance where balance.shopId = :shopId")
    Optional<SellerBalanceEntity> findLocked(@Param("shopId") UUID shopId);
}
