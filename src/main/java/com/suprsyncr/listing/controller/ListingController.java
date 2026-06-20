package com.suprsyncr.listing.controller;

import com.suprsyncr.common.dto.ApiResponse;
import com.suprsyncr.listing.dto.BulkListingResponse;
import com.suprsyncr.listing.dto.CreateListingRequest;
import com.suprsyncr.listing.dto.ListingDto;
import com.suprsyncr.listing.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for listing management.
 * Exposes endpoints for sellers to publish products to marketplaces and manage listing lifecycle.
 * 
 * Requirements: 12, 13, 96
 */
@RestController
@RequestMapping("/api/v1/listings")
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Listings", description = "Product listing management across marketplace platforms")
public class ListingController {
    
    private final ListingService listingService;
    
    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }
    
    @PostMapping
    @Operation(summary = "Create listings", description = "Publishes a product to one or more marketplace platforms")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Listings created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product or platform not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Listing already exists for product on platform"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<BulkListingResponse>> createListings(
        @Valid @RequestBody CreateListingRequest request
    ) {
        BulkListingResponse bulkResponse = listingService.createListings(request);
        ApiResponse<BulkListingResponse> response = new ApiResponse<>(
            true,
            bulkResponse,
            "Listings created successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{listingId}")
    @Operation(summary = "Get listing", description = "Retrieves a single listing by ID")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Listing retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<ListingDto>> getListing(@PathVariable Long listingId) {
        ListingDto listing = listingService.getListing(listingId);
        ApiResponse<ListingDto> response = new ApiResponse<>(
            true,
            listing,
            "Listing retrieved successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/product/{productId}")
    @Operation(summary = "Get listings by product", description = "Retrieves all listings for a specific product across all platforms")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Listings retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<ListingDto>>> getListingsByProduct(@PathVariable Long productId) {
        List<ListingDto> listings = listingService.getListingsByProduct(productId);
        ApiResponse<List<ListingDto>> response = new ApiResponse<>(
            true,
            listings,
            "Listings retrieved successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/platform/{platformId}")
    @Operation(summary = "Get listings by platform", description = "Retrieves all listings for a specific marketplace platform")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Listings retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<ListingDto>>> getListingsByPlatform(@PathVariable Long platformId) {
        List<ListingDto> listings = listingService.getListingsByPlatform(platformId);
        ApiResponse<List<ListingDto>> response = new ApiResponse<>(
            true,
            listings,
            "Listings retrieved successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{listingId}/sync")
    @Operation(summary = "Sync listing", description = "Synchronizes listing data with the marketplace platform")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Listing synchronized successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Marketplace API error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<ListingDto>> syncListing(@PathVariable Long listingId) {
        ListingDto listing = listingService.syncListing(listingId);
        ApiResponse<ListingDto> response = new ApiResponse<>(
            true,
            listing,
            "Listing synchronized successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{listingId}")
    @Operation(summary = "Delist product", description = "Removes a product listing from the marketplace platform")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product delisted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Marketplace API error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Void>> delistProduct(@PathVariable Long listingId) {
        listingService.delistProduct(listingId);
        ApiResponse<Void> response = new ApiResponse<>(
            true,
            null,
            "Product delisted successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{listingId}/retry")
    @Operation(summary = "Retry failed listing", description = "Retries publishing a failed listing to the marketplace")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Listing retry completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Marketplace API error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<ListingDto>> retryFailedListing(@PathVariable Long listingId) {
        ListingDto listing = listingService.retryFailedListing(listingId);
        ApiResponse<ListingDto> response = new ApiResponse<>(
            true,
            listing,
            "Listing retry completed",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
}

