package ai.synalix.synalixai.dto.user;

import ai.synalix.synalixai.enums.UserRole;
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
public class UpdateUserRoleRequest {

    @NotNull(message = "Role cannot be null")
    private UserRole role;

}