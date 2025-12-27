package ai.synalix.synalixai.dto.gpu;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Update user GPU permission request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserGpuPermissionRequest {

    @NotNull(message = "GPU IDs cannot be null")
    private List<Long> gpuIds;
}

