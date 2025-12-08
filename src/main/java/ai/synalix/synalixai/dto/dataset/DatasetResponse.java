package ai.synalix.synalixai.dto.dataset;

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
     * Size in bytes
     */
    private Long size;

    /**
     * Storage path or URL
     */
    private String path;

    /**
     * User ID who uploaded the dataset
     */
    private UUID uploadedBy;

    /**
     * Timestamp when the dataset was created
     */
    private LocalDateTime createdAt;
}