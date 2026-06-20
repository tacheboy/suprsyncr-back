package com.suprsyncr.listing.repository;

import com.suprsyncr.listing.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Listing entity.
 * Provides data access methods for managing product listings across marketplace platforms.
 * 
 * Requirements: 12, 13, 58, 94
 */
@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    
    /**
     * Find all listings for a specific product.
     * 
     * @param productId the product ID
     * @return list of listings for the product
     */
    List<Listing> findByProductId(Long productId);
    
    /**
     * Find all listings for a specific platform.
     * 
     * @param platformId the platform ID
     * @return list of listings for the platform
     */
    List<Listing> findByPlatformId(Long platformId);
    
    /**
     * Find a listing by product and platform combination.
     * This is useful for checking if a product is already listed on a specific platform
     * to prevent duplicate listings.
     * 
     * @param productId the product ID
     * @param platformId the platform ID
     * @return Optional containing the listing if found
     */
    Optional<Listing> findByProductIdAndPlatformId(Long productId, Long platformId);
}

