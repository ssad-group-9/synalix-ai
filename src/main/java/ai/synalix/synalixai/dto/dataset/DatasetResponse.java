package ai.synalix.synalixai.dto.dataset;

import ai.synalix.synalixai.enums.DatasetStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for dataset information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetResponse {

    /**
     * Unique identifier of the dataset
     */
    private UUID id;

    /**
     * Display name of the dataset
     */
    private String name;

    /**
     * Description of the dataset
     */
    private String description;

    /**
     * Current status of the dataset
     */
    private DatasetStatus status;

    /**
     * Original filename uploaded by user
     */
    private String originalFilename;

    /**
     * File size in bytes
     */
    private Long fileSize;

    /**
     * MIME type of the file
     */
    private String contentType;

    /**
     * Training set ratio (0.0 - 1.0)
     */
    private Double trainRatio;

    /**
     * Test set ratio (0.0 - 1.0)
     */
    private Double testRatio;

    /**
     * Evaluation set ratio (0.0 - 1.0)
     */
    private Double evalRatio;

    /**
     * Timestamp when the dataset was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the dataset was last updated
     */
    private LocalDateTime updatedAt;
}