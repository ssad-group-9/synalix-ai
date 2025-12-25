package ai.synalix.synalixai.dto.file;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create file request DTO (metadata only).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFileRequest {

    /**
     * Original filename provided by client.
     */
    @Size(max = 255, message = "originalFilename cannot exceed 255 characters")
    private String originalFilename;

    /**
     * Content type provided by client.
     */
    @Size(max = 100, message = "contentType cannot exceed 100 characters")
    private String contentType;
}