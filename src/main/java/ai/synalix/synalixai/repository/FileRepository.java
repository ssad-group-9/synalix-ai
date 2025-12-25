package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.Files;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for FileObject entities.
 */
public interface FileRepository extends JpaRepository<Files, UUID> {
}