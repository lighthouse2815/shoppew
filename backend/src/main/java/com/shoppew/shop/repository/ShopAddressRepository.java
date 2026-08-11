package com.shoppew.shop.repository;

import com.shoppew.shop.entity.ShopAddressEntity;
import com.shoppew.shop.entity.ShopAddressType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopAddressRepository extends JpaRepository<ShopAddressEntity, UUID> {

    List<ShopAddressEntity> findAllByShopIdOrderByAddressTypeAscDefaultAddressDescCreatedAtDesc(UUID shopId);

    List<ShopAddressEntity> findAllByShopIdAndAddressTypeOrderByDefaultAddressDescCreatedAtDesc(
            UUID shopId,
            ShopAddressType addressType);

    Optional<ShopAddressEntity> findByIdAndShopId(UUID id, UUID shopId);

    boolean existsByShopIdAndAddressType(UUID shopId, ShopAddressType addressType);

    @Modifying(clearAutomatically = true)
    @Query("""
            update ShopAddressEntity address
            set address.defaultAddress = false
            where address.shop.id = :shopId and address.addressType = :type and address.defaultAddress = true
            """)
    int clearDefault(@Param("shopId") UUID shopId, @Param("type") ShopAddressType type);
}
