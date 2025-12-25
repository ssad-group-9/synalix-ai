package ai.synalix.synalixai.dto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import ai.synalix.synalixai.enums.CheckpointType;

/**
 * Checkpoint response DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckpointResponse {
    private UUID id;
    private UUID modelId;
    private String name;
    private String path;
    private String taskId;
    private CheckpointType type;
    private LocalDateTime createdAt;
}