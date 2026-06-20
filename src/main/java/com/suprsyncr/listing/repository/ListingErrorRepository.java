package com.suprsyncr.listing.repository;

import com.suprsyncr.listing.entity.ListingError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ListingError entity.
 * Provides data access methods for tracking and managing listing publication errors.
 * 
 * Requirements: 12, 13, 58, 94
 */
@Repository
public interface ListingErrorRepository extends JpaRepository<ListingError, Long> {
    
    /**
     * Find all errors for a specific listing.
     * This helps troubleshoot issues with listing publication and synchronization.
     * 
     * @param listingId the listing ID
     * @return list of errors for the listing
     */
    List<ListingError> findByListingId(Long listingId);
}

