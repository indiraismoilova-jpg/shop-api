package com.example.shop.repository;

import com.example.shop.model.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByToken(String token);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken r
            SET r.revoked = true
            WHERE r.user.id = :userId
            AND r.revoked = false
            """)
    int revokeAllByUserId(@Param("userId") Long userId);
}
