package com.shoppew.order.repository;

import com.shoppew.order.entity.OrderAddressEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAddressRepository extends JpaRepository<OrderAddressEntity, UUID> {}
