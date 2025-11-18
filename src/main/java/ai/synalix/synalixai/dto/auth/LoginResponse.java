package ai.synalix.synalixai.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Login response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    @ToString.Exclude
    private String accessToken;

    @ToString.Exclude
    private String refreshToken;

    private String tokenType;
    
    private Long expiresIn;
}