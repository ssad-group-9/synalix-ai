package ai.synalix.synalixai.dto.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for presigned URL
 * Used for both upload and download operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {

    /**
     * The presigned URL for upload or download
     */
    private String url;

    /**
     * HTTP method to use (PUT for upload, GET for download)
     */
    private String method;

    /**
     * Expiration time of the presigned URL
     */
    private LocalDateTime expiresAt;
}