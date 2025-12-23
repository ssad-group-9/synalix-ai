package ai.synalix.synalixai.dto.resource;

import ai.synalix.synalixai.enums.ResourceStatus;
import lombok.Data;

/**
 * Response DTO for resource details
 */
@Data
public class ResourceResponse {
    private Long id;
    private String name;
    private ResourceStatus status;
    private Integer memoryTotal;
    private Integer memoryUsed;
}
