package com.suprsyncr.listing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.auth.service.AuthService;
import com.suprsyncr.common.exception.MarketplaceApiException;
import com.suprsyncr.common.exception.ResourceNotFoundException;
import com.suprsyncr.common.exception.ValidationException;
import com.suprsyncr.integration.connector.ConnectorRegistry;
import com.suprsyncr.integration.connector.MarketplaceConnector;
import com.suprsyncr.listing.dto.BulkListingResponse;
import com.suprsyncr.listing.dto.CreateListingRequest;
import com.suprsyncr.listing.dto.ListingDto;
import com.suprsyncr.listing.dto.ListingErrorDto;
import com.suprsyncr.listing.entity.Listing;
import com.suprsyncr.listing.entity.ListingError;
import com.suprsyncr.listing.entity.ListingStatus;
import com.suprsyncr.listing.repository.ListingErrorRepository;
import com.suprsyncr.listing.repository.ListingRepository;
import com.suprsyncr.product.entity.Product;
import com.suprsyncr.product.repository.ProductRepository;
import com.suprsyncr.seller.entity.ConnectionStatus;
import com.suprsyncr.seller.entity.SellerPlatform;
import com.suprsyncr.seller.repository.SellerPlatformRepository;
import com.suprsyncr.seller.service.CredentialEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of ListingService for managing product listings across marketplace platforms.
 * Handles listing creation, synchronization, delisting, and error recovery with transactional support.
 * 
 * Requirements: 12, 13, 57, 58, 70, 95
 */
@Service
@Transactional
public class ListingServiceImpl implements ListingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ListingServiceImpl.class);
    
    private final ListingRepository listingRepository;
    private final ListingErrorRepository listingErrorRepository;
    private final ProductRepository productRepository;
    private final SellerPlatformRepository sellerPlatformRepository;
    private final ConnectorRegistry connectorRegistry;
    private final CredentialEncryptionService encryptionService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    
    public ListingServiceImpl(
            ListingRepository listingRepository,
            ListingErrorRepository listingErrorRepository,
            ProductRepository productRepository,
            SellerPlatformRepository sellerPlatformRepository,
            ConnectorRegistry connectorRegistry,
            CredentialEncryptionService encryptionService,
            AuthService authService,
            ObjectMapper objectMapper) {
        this.listingRepository = listingRepository;
        this.listingErrorRepository = listingErrorRepository;
        this.productRepository = productRepository;
        this.sellerPlatformRepository = sellerPlatformRepository;
        this.connectorRegistry = connectorRegistry;
        this.encryptionService = encryptionService;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public BulkListingResponse createListings(CreateListingRequest request) {
        // Verify product exists and belongs to seller
        Long sellerId = authService.getCurrentUser().getId();
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));
        
        if (!product.getSeller().getUser().getId().equals(sellerId)) {
            throw new ValidationException("Product does not belong to the current seller");
        }
        
        List<ListingDto> listings = new ArrayList<>();
        int successful = 0;
        int failed = 0;
        
        // Process each platform
        for (Long platformId : request.platformIds()) {
            try {
                // Verify platform connection exists and is active
                SellerPlatform platform = sellerPlatformRepository.findById(platformId)
                        .orElseThrow(() -> new ResourceNotFoundException("Platform not found with id: " + platformId));
                
                if (!platform.getSeller().getUser().getId().equals(sellerId)) {
                    throw new ValidationException("Platform does not belong to the current seller");
                }
                
                if (platform.getStatus() != ConnectionStatus.CONNECTED) {
                    throw new ValidationException("Platform connection is not active. Status: " + platform.getStatus());
                }
                
                // Prevent duplicate listings
                if (listingRepository.findByProductIdAndPlatformId(product.getId(), platformId).isPresent()) {
                    throw new ValidationException("Product is already listed on this platform");
                }
                
                // Get connector for the platform
                MarketplaceConnector connector = connectorRegistry.getConnector(platform.getPlatformType());
                
                // Decrypt credentials
                String decryptedCredentials = encryptionService.decrypt(platform.getEncryptedCredentials());
                Map<String, String> credentials = parseCredentials(decryptedCredentials);
                
                // Create listing entity
                Listing listing = new Listing();
                listing.setProduct(product);
                listing.setPlatform(platform);
                listing.setStatus(ListingStatus.PENDING);
                
                try {
                    // Call connector to publish product
                    String externalProductId = connector.publishProduct(product, credentials);
                    
                    // On success: update listing with PUBLISHED status and external ID
                    listing.setExternalProductId(externalProductId);
                    listing.setStatus(ListingStatus.PUBLISHED);
                    listing.setPublishedAt(LocalDateTime.now());
                    listing.setLastSyncedAt(LocalDateTime.now());
                    
                    listingRepository.save(listing);
                    successful++;
                    
                    logger.info("Successfully published product {} to platform {}", product.getId(), platformId);
                    
                } catch (Exception e) {
                    // On failure: set status to FAILED and create ListingError
                    listing.setStatus(ListingStatus.FAILED);
                    listingRepository.save(listing);
                    
                    ListingError error = new ListingError();
                    error.setListing(listing);
                    error.setErrorMessage(e.getMessage());
                    error.setErrorDetails(getStackTraceAsString(e));
                    error.setResolved(false);
                    listingErrorRepository.save(error);
                    
                    failed++;
                    
                    logger.error("Failed to publish product {} to platform {}: {}", 
                            product.getId(), platformId, e.getMessage());
                }
                
                listings.add(toDto(listing));
                
            } catch (Exception e) {
                // Handle validation errors and other exceptions
                failed++;
                logger.error("Error processing listing for platform {}: {}", platformId, e.getMessage());
                
                // Create a failed listing entry
                SellerPlatform platform = sellerPlatformRepository.findById(platformId).orElse(null);
                if (platform != null) {
                    Listing listing = new Listing();
                    listing.setProduct(product);
                    listing.setPlatform(platform);
                    listing.setStatus(ListingStatus.FAILED);
                    listingRepository.save(listing);
                    
                    ListingError error = new ListingError();
                    error.setListing(listing);
                    error.setErrorMessage(e.getMessage());
                    error.setErrorDetails(getStackTraceAsString(e));
                    error.setResolved(false);
                    listingErrorRepository.save(error);
                    
                    listings.add(toDto(listing));
                }
            }
        }
        
        return new BulkListingResponse(
                request.platformIds().size(),
                successful,
                failed,
                listings
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public ListingDto getListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
        // Verify ownership
        Long sellerId = authService.getCurrentUser().getId();
        if (!listing.getProduct().getSeller().getUser().getId().equals(sellerId)) {
            throw new ValidationException("Listing does not belong to the current seller");
        }
        
        return toDto(listing);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ListingDto> getListingsByProduct(Long productId) {
        // Verify product exists and belongs to seller
        Long sellerId = authService.getCurrentUser().getId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        if (!product.getSeller().getUser().getId().equals(sellerId)) {
            throw new ValidationException("Product does not belong to the current seller");
        }
        
        List<Listing> listings = listingRepository.findByProductId(productId);
        return listings.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ListingDto> getListingsByPlatform(Long platformId) {
        // Verify platform belongs to seller
        Long sellerId = authService.getCurrentUser().getId();
        SellerPlatform platform = sellerPlatformRepository.findById(platformId)
                .orElseThrow(() -> new ResourceNotFoundException("Platform not found with id: " + platformId));
        
        if (!platform.getSeller().getUser().getId().equals(sellerId)) {
            throw new ValidationException("Platform does not belong to the current seller");
        }
        
        List<Listing> listings = listingRepository.findByPlatformId(platformId);
        return listings.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public ListingDto syncListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
        // Verify ownership
        Long sellerId = authService.getCurrentUser().getId();
        if (!listing.getProduct().getSeller().getUser().getId().equals(sellerId)) {
            throw new ValidationException("Listing does not belong to the current seller");
        }
        
        // Update last synced timestamp
        listing.setLastSyncedAt(LocalDateTime.now());
        listingRepository.save(listing);
        
        logger.info("Synced listing {} at {}", listingId, listing.getLastSyncedAt());
        
        return toDto(listing);
    }
    
    @Override
    public void delistProduct(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
        // Verify ownership
        Long sellerId = authService.getCurrentUser().getId();
        if (!listing.getProduct().getSeller().getUser().getId().equals(sellerId)) {
            throw new ValidationException("Listing does not belong to the current seller");
        }
        
        // Only delist if currently published
        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new ValidationException("Can only delist published listings. Current status: " + listing.getStatus());
        }
        
        try {
            // Get connector and credentials
            SellerPlatform platform = listing.getPlatform();
            MarketplaceConnector connector = connectorRegistry.getConnector(platform.getPlatformType());
            
            String decryptedCredentials = encryptionService.decrypt(platform.getEncryptedCredentials());
            Map<String, String> credentials = parseCredentials(decryptedCredentials);
            
            // Call connector to delist product
            connector.delistProduct(listing.getExternalProductId(), credentials);
            
            // Update status to DELISTED
            listing.setStatus(ListingStatus.DELISTED);
            listingRepository.save(listing);
            
            logger.info("Successfully delisted product from listing {}", listingId);
            
        } catch (Exception e) {
            logger.error("Failed to delist product from listing {}: {}", listingId, e.getMessage());
            throw new MarketplaceApiException(
                    listing.getPlatform().getPlatformType().name(),
                    null,
                    "Failed to delist product: " + e.getMessage(),
                    e
            );
        }
    }
    
    @Override
    public ListingDto retryFailedListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
        // Verify ownership
        Long sellerId = authService.getCurrentUser().getId();
        if (!listing.getProduct().getSeller().getUser().getId().equals(sellerId)) {
            throw new ValidationException("Listing does not belong to the current seller");
        }
        
        // Only retry failed listings
        if (listing.getStatus() != ListingStatus.FAILED) {
            throw new ValidationException("Can only retry failed listings. Current status: " + listing.getStatus());
        }
        
        try {
            // Get connector and credentials
            SellerPlatform platform = listing.getPlatform();
            MarketplaceConnector connector = connectorRegistry.getConnector(platform.getPlatformType());
            
            String decryptedCredentials = encryptionService.decrypt(platform.getEncryptedCredentials());
            Map<String, String> credentials = parseCredentials(decryptedCredentials);
            
            // Attempt to publish again
            String externalProductId = connector.publishProduct(listing.getProduct(), credentials);
            
            // On success: update listing
            listing.setExternalProductId(externalProductId);
            listing.setStatus(ListingStatus.PUBLISHED);
            listing.setPublishedAt(LocalDateTime.now());
            listing.setLastSyncedAt(LocalDateTime.now());
            listingRepository.save(listing);
            
            logger.info("Successfully retried and published listing {}", listingId);
            
        } catch (Exception e) {
            // On failure: create new error entry
            ListingError error = new ListingError();
            error.setListing(listing);
            error.setErrorMessage(e.getMessage());
            error.setErrorDetails(getStackTraceAsString(e));
            error.setResolved(false);
            listingErrorRepository.save(error);
            
            logger.error("Failed to retry listing {}: {}", listingId, e.getMessage());
            
            throw new MarketplaceApiException(
                    listing.getPlatform().getPlatformType().name(),
                    null,
                    "Failed to retry listing: " + e.getMessage(),
                    e
            );
        }
        
        return toDto(listing);
    }
    
    /**
     * Converts a Listing entity to a ListingDto.
     */
    private ListingDto toDto(Listing listing) {
        List<ListingErrorDto> errorDtos = listing.getErrors().stream()
                .map(error -> new ListingErrorDto(
                        error.getId(),
                        error.getErrorMessage(),
                        error.getErrorDetails(),
                        error.isResolved(),
                        error.getCreatedAt()
                ))
                .collect(Collectors.toList());
        
        return new ListingDto(
                listing.getId(),
                listing.getProduct().getId(),
                listing.getProduct().getName(),
                listing.getPlatform().getId(),
                listing.getPlatform().getPlatformType(),
                listing.getExternalProductId(),
                listing.getStatus(),
                listing.getPublishedAt(),
                listing.getLastSyncedAt(),
                errorDtos
        );
    }
    
    /**
     * Parses JSON credentials string to a Map.
     */
    private Map<String, String> parseCredentials(String credentialsJson) {
        try {
            return objectMapper.readValue(credentialsJson, 
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse credentials", e);
        }
    }
    
    /**
     * Converts exception stack trace to string.
     */
    private String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 2000) { // Limit stack trace length
                sb.append("\t... (truncated)");
                break;
            }
        }
        return sb.toString();
    }
}

