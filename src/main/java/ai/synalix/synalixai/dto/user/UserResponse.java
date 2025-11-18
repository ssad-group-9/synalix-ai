package ai.synalix.synalixai.dto.user;

import ai.synalix.synalixai.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String nickname;
    private String email;
    private UserRole role;
    private Boolean enabled;
    private LocalDateTime createdAt;
}