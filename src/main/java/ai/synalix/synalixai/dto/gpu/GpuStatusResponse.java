package ai.synalix.synalixai.dto.gpu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GPU status response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpuStatusResponse {

    /**
     * Total GPU count
     */
    @JsonProperty("total_gpus")
    private int totalGpus;

    /**
     * Available GPU count
     */
    @JsonProperty("available_gpus")
    private int availableGpus;

    /**
     * GPU details list
     */
    @JsonProperty("gpu_details")
    private List<ResourceDetail> gpuDetails;
}