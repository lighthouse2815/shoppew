package com.shoppew.address.repository;

import com.shoppew.address.entity.UserAddressEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAddressRepository extends JpaRepository<UserAddressEntity, UUID> {

    List<UserAddressEntity> findAllByUserIdOrderByDefaultAddressDescCreatedAtDesc(UUID userId);

    Optional<UserAddressEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserId(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserAddressEntity address
            set address.defaultAddress = false
            where address.user.id = :userId and address.defaultAddress = true
            """)
    int clearDefault(@Param("userId") UUID userId);
}
