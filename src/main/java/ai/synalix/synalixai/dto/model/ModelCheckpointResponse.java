package ai.synalix.synalixai.dto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for model checkpoint information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelCheckpointResponse {

    /**
     * Unique identifier of the checkpoint
     */
    private UUID id;

    /**
     * Display name of the checkpoint
     */
    private String name;

    /**
     * Description of the checkpoint
     */
    private String description;

    /**
     * File size in bytes
     */
    private Long fileSize;

    /**
     * ID of the base model used for fine-tuning
     */
    private UUID baseModelId;

    /**
     * Name of the base model used for fine-tuning
     */
    private String baseModelName;

    /**
     * ID of the dataset used for training
     */
    private UUID datasetId;

    /**
     * Name of the dataset used for training
     */
    private String datasetName;

    /**
     * Whether the checkpoint upload is complete
     */
    private boolean uploadComplete;

    /**
     * Timestamp when the checkpoint was created
     */
    private LocalDateTime createdAt;
}