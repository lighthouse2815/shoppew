package com.shoppew.auth.repository;

import com.shoppew.auth.entity.UserSessionEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct session from UserSessionEntity session
            join fetch session.user user
            left join fetch user.roles
            where session.refreshTokenHash = :hash
            """)
    Optional<UserSessionEntity> findForRefresh(@Param("hash") String hash);

    @Query("""
            select session from UserSessionEntity session
            where session.user.id = :userId and session.revokedAt is null and session.expiresAt > :now
            order by session.lastUsedAt desc
            """)
    List<UserSessionEntity> findActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    long countByUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);

    Optional<UserSessionEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID id, UUID userId, Instant now);

    @Modifying
    @Query("""
            update UserSessionEntity session
            set session.revokedAt = :now, session.revokeReason = :reason
            where session.tokenFamilyId = :familyId and session.revokedAt is null
            """)
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now, @Param("reason") String reason);

    @Modifying
    @Query("""
            update UserSessionEntity session
            set session.revokedAt = :now, session.revokeReason = :reason
            where session.user.id = :userId and session.revokedAt is null
            """)
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now, @Param("reason") String reason);
}
