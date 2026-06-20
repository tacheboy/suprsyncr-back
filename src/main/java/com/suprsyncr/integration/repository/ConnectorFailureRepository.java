package com.suprsyncr.integration.repository;

import com.suprsyncr.integration.entity.ConnectorFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ConnectorFailure entity.
 */
@Repository
public interface ConnectorFailureRepository extends JpaRepository<ConnectorFailure, Long> {
    
    /**
     * Find all unresolved failures for a specific platform.
     *
     * @param platformId the platform ID
     * @return list of unresolved connector failures
     */
    List<ConnectorFailure> findByPlatformIdAndResolvedFalse(Long platformId);
}
