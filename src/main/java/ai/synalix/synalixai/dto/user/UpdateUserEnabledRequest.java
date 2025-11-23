package ai.synalix.synalixai.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update user request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserEnabledRequest {

    @NotNull(message = "Enabled cannot be null")
    private Boolean enabled;

}