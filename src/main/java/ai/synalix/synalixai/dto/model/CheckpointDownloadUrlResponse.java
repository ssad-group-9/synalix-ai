package ai.synalix.synalixai.dto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO to frontend that includes MinIO download URL and backend message
 * if any.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckpointDownloadUrlResponse {

    /**
     * Presigned download URL from MinIO.
     */
    private String downloadUrl;

    /**
     * Optional backend response payload (if backend returns something useful).
     */
    private Object backendResponse;
}