package ai.synalix.synalixai.dto.file;

import ai.synalix.synalixai.enums.FileStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * File response DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {

    private UUID id;
    private String storageKey;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private FileStatus status;
    private UUID createdBy;
    private LocalDateTime createdAt;
}