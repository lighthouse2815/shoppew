package com.shoppew.notification.repository;

import com.shoppew.notification.entity.PushDeviceEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PushDeviceRepository extends JpaRepository<PushDeviceEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PushDeviceEntity> findByTargetHash(String targetHash);

    List<PushDeviceEntity> findAllByUser_IdAndActiveTrueOrderByLastSeenAtDesc(UUID userId);
}
