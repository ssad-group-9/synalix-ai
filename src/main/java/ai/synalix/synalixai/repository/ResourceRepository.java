package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Resource repository interface
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    
}
