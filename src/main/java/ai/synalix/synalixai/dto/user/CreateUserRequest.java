package ai.synalix.synalixai.dto.user;

import ai.synalix.synalixai.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Create user request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(min = 6, max = 255, message = "Password must be at least 6 characters")
    @ToString.Exclude
    private String password; // Optional - if not provided, a random password will be generated

    @NotBlank(message = "Nickname cannot be blank")
    @Size(max = 100, message = "Nickname cannot exceed 100 characters")
    private String nickname;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @NotNull(message = "Role cannot be null")
    private UserRole role;
}