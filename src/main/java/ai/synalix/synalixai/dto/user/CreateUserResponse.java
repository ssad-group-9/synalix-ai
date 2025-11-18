package ai.synalix.synalixai.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user creation, includes generated password
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserResponse {
    
    private UserResponse user;
    private String generatedPassword;
}