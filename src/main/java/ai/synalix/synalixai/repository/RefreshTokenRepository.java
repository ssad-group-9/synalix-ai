package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh token data access layer interface
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by token string
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all tokens by user ID
     */
    List<RefreshToken> findByUserId(UUID userId);

    /**
     * Find valid tokens by user ID (not revoked and not expired)
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Find expired tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt <= :now")
    List<RefreshToken> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Revoke all tokens for a user
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId")
    int revokeAllTokensByUserId(@Param("userId") UUID userId);

    /**
     * Revoke specific token
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.token = :token")
    int revokeTokenByToken(@Param("token") String token);

    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt <= :expirationTime")
    int deleteExpiredTokens(@Param("expirationTime") LocalDateTime expirationTime);

    /**
     * Delete revoked tokens older than specified time
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true AND rt.createdAt <= :cutoffTime")
    int deleteRevokedTokensOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count valid tokens for user
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    long countValidTokensByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}