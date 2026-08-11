package com.shoppew.shop.repository;

import com.shoppew.shop.entity.ShopSettingsEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopSettingsRepository extends JpaRepository<ShopSettingsEntity, UUID> {}
