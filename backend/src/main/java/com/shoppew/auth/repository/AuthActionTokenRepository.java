package com.shoppew.auth.repository;

import com.shoppew.auth.entity.AuthActionTokenEntity;
import com.shoppew.auth.entity.AuthActionTokenType;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthActionTokenRepository extends JpaRepository<AuthActionTokenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token from AuthActionTokenEntity token
            join fetch token.user
            where token.tokenHash = :tokenHash and token.type = :type
            """)
    Optional<AuthActionTokenEntity> findForUse(
            @Param("tokenHash") String tokenHash,
            @Param("type") AuthActionTokenType type);

    @Modifying
    @Query("""
            update AuthActionTokenEntity token
            set token.consumedAt = :now
            where token.user.id = :userId
              and token.type = :type
              and token.consumedAt is null
            """)
    int consumeOutstanding(
            @Param("userId") UUID userId,
            @Param("type") AuthActionTokenType type,
            @Param("now") Instant now);
}
