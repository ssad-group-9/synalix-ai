package ai.synalix.synalixai.entity;

import ai.synalix.synalixai.enums.UserRole;
import ai.synalix.synalixai.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity class
 */
@Entity
@Table(name = "users", 
       uniqueConstraints = @UniqueConstraint(columnNames = "username"))
@Data
@NoArgsConstructor
public class User {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username length must be between 3 and 50 characters")
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank(message = "Password hash cannot be blank")
    @Column(name = "password_hash", nullable = false)
    @ToString.Exclude
    private String passwordHash;

    @NotBlank(message = "Nickname cannot be blank")
    @Size(max = 100, message = "Nickname length cannot exceed 100 characters")
    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email length cannot exceed 255 characters")
    @Column(name = "email", length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ENABLED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if user is an administrator
     */
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.role);
    }

    /**
     * Check if user is enabled
     */
    public boolean isEnabled() {
        return UserStatus.ENABLED.equals(this.status);
    }
}