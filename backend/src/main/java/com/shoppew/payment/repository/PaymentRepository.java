package com.shoppew.payment.repository;

import com.shoppew.payment.entity.PaymentEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID>, JpaSpecificationExecutor<PaymentEntity> {
    Optional<PaymentEntity> findByCheckoutGroup_Id(UUID checkoutId);
    Optional<PaymentEntity> findByIdAndCheckoutGroup_User_Id(UUID paymentId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from PaymentEntity payment where payment.providerReference = :reference")
    Optional<PaymentEntity> findLockedByProviderReference(@Param("reference") String providerReference);
}
