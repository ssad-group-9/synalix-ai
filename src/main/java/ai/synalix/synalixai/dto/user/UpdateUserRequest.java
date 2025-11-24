package ai.synalix.synalixai.dto.user;

import ai.synalix.synalixai.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update user request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(max = 100, message = "Nickname cannot exceed 100 characters")
    private String nickname;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    private Boolean enabled;

    private UserRole role;

}