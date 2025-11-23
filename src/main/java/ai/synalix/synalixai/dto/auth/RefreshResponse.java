package ai.synalix.synalixai.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Refresh response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshResponse {

    @ToString.Exclude
    private String accessToken;

}