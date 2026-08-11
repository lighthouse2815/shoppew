package com.shoppew.voucher.repository;

import com.shoppew.voucher.entity.VoucherUsageEntity;
import com.shoppew.voucher.entity.VoucherUsageStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsageEntity, UUID> {
    @Query("""
            select count(distinct usage.checkoutGroup.id) from VoucherUsageEntity usage
            where usage.voucher.id = :voucherId and usage.user.id = :userId and usage.status <> 'RELEASED'
            """)
    long countActiveApplications(@Param("voucherId") UUID voucherId, @Param("userId") UUID userId);

    @Query("select usage from VoucherUsageEntity usage join fetch usage.voucher where usage.checkoutGroup.id = :checkoutId")
    List<VoucherUsageEntity> findAllByCheckout(@Param("checkoutId") UUID checkoutId);

    @Query("select usage from VoucherUsageEntity usage join fetch usage.voucher where usage.order.id = :orderId")
    List<VoucherUsageEntity> findAllByOrder(@Param("orderId") UUID orderId);

    boolean existsByVoucher_IdAndCheckoutGroup_IdAndStatusIn(
            UUID voucherId, UUID checkoutId, Collection<VoucherUsageStatus> statuses);
}
