package ai.synalix.synalixai.dto.gpu;

import com.fasterxml.jackson.annotation.JsonProperty;

import ai.synalix.synalixai.enums.ResourceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single GPU detail DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDetail {

    /**
     * GPU id
     */
    @JsonProperty("id")
    private Long id;

    /**
     * GPU name
     */
    @JsonProperty("name")
    private String name;

    /**
     * Memory usage like "447/15360MB"
     */
    @JsonProperty("memory_used")
    private String memoryUsed;

    /**
     * Utilization like "0%"
     */
    @JsonProperty("utilization")
    private String utilization;

    /**
     * Temperature like "33°C"
     */
    @JsonProperty("temperature")
    private String temperature;

    @JsonProperty("status")
    private ResourceStatus status;
}