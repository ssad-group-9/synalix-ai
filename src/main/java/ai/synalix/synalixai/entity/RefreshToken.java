package ai.synalix.synalixai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Refresh token entity class
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
public class RefreshToken {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "User ID cannot be null")
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotBlank(message = "Token cannot be blank")
    @Column(name = "token", nullable = false, unique = true)
    @ToString.Exclude
    private String token;

    @NotNull(message = "Expiry date cannot be null")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Association with User entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * Check if token is valid (not revoked and not expired)
     */
    public boolean isValid() {
        return !this.revoked && !isExpired();
    }

    /**
     * Revoke token
     */
    public void revoke() {
        this.revoked = true;
    }
}