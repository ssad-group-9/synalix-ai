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
     * Memory usage like "447"
     */
    @JsonProperty("memory_used")
    private Integer memoryUsed;

    /**
     * Memory usage like"15360"
     */
    @JsonProperty("memory_total")
    private Integer memoryTotal;

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