package ai.synalix.synalixai.dto.gpu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * User GPU permission response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGpuPermissionResponse {
    private UUID userId;
    private String username;
    private String nickname;
    private List<Long> allowedGpuIds;
}

