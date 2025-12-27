package ai.synalix.synalixai.dto.gpu;

import ai.synalix.synalixai.enums.ResourceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GPU resource response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpuResourceResponse {
    private Long id;
    private String name;
    private ResourceStatus status;
    private Integer memoryTotal;
    private Integer memoryUsed;
    private Integer memoryFree;
}

