package com.suprsyncr.listing.service;

import com.suprsyncr.listing.dto.BulkListingResponse;
import com.suprsyncr.listing.dto.CreateListingRequest;
import com.suprsyncr.listing.dto.ListingDto;

import java.util.List;

/**
 * Service interface for managing product listings across marketplace platforms.
 * Handles listing creation, synchronization, delisting, and error recovery.
 * 
 * Requirements: 12, 13, 57, 58, 70, 95
 */
public interface ListingService {
    
    /**
     * Creates listings for a product across multiple platforms.
     * Verifies product ownership, validates platform connections, prevents duplicates,
     * and publishes to each platform using the appropriate connector.
     * 
     * @param request the listing creation request containing product ID and platform IDs
     * @return bulk listing response with success/failure counts and detailed results
     */
    BulkListingResponse createListings(CreateListingRequest request);
    
    /**
     * Retrieves a listing by its ID.
     * 
     * @param listingId the listing ID
     * @return the listing DTO
     */
    ListingDto getListing(Long listingId);
    
    /**
     * Retrieves all listings for a specific product.
     * 
     * @param productId the product ID
     * @return list of listings for the product
     */
    List<ListingDto> getListingsByProduct(Long productId);
    
    /**
     * Retrieves all listings for a specific platform.
     * 
     * @param platformId the platform ID
     * @return list of listings for the platform
     */
    List<ListingDto> getListingsByPlatform(Long platformId);
    
    /**
     * Synchronizes a listing with the marketplace platform.
     * Fetches the latest data from the marketplace and updates the last synced timestamp.
     * 
     * @param listingId the listing ID
     * @return the updated listing DTO
     */
    ListingDto syncListing(Long listingId);
    
    /**
     * Delists a product from the marketplace platform.
     * Calls the connector to remove the product and updates the listing status to DELISTED.
     * 
     * @param listingId the listing ID
     */
    void delistProduct(Long listingId);
    
    /**
     * Retries publishing a failed listing.
     * Attempts to publish the product again using the marketplace connector.
     * 
     * @param listingId the listing ID
     * @return the updated listing DTO
     */
    ListingDto retryFailedListing(Long listingId);
}

