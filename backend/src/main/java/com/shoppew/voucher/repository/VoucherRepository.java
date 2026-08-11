package com.shoppew.voucher.repository;

import com.shoppew.voucher.entity.VoucherEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherRepository extends JpaRepository<VoucherEntity, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    @EntityGraph(attributePaths = {"shop", "products", "categories", "paymentProviders"})
    @Query("select distinct voucher from VoucherEntity voucher where upper(voucher.code) in :codes")
    List<VoucherEntity> findDetailedByCodes(@Param("codes") Collection<String> codes);

    @EntityGraph(attributePaths = {"shop", "products", "categories", "paymentProviders"})
    List<VoucherEntity> findAllByShop_IdOrderByCreatedAtDesc(UUID shopId);

    @EntityGraph(attributePaths = {"shop", "products", "categories", "paymentProviders"})
    List<VoucherEntity> findAllByShopIsNullOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select voucher from VoucherEntity voucher where voucher.id = :id")
    Optional<VoucherEntity> findLocked(@Param("id") UUID id);
}
