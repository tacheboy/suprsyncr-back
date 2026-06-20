package com.suprsyncr.seller.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.auth.entity.User;
import com.suprsyncr.auth.service.AuthService;
import com.suprsyncr.common.exception.ResourceNotFoundException;
import com.suprsyncr.common.exception.ValidationException;
import com.suprsyncr.integration.connector.ConnectionTestResult;
import com.suprsyncr.integration.connector.ConnectorRegistry;
import com.suprsyncr.integration.connector.MarketplaceConnector;
import com.suprsyncr.seller.dto.*;
import com.suprsyncr.seller.entity.*;
import com.suprsyncr.seller.repository.SellerPlatformRepository;
import com.suprsyncr.seller.repository.SellerRepository;
import com.suprsyncr.seller.repository.SellerWarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of SellerService.
 */
@Service
@Transactional
public class SellerServiceImpl implements SellerService {
    
    private final SellerRepository sellerRepository;
    private final SellerWarehouseRepository warehouseRepository;
    private final SellerPlatformRepository platformRepository;
    private final AuthService authService;
    private final CredentialEncryptionService encryptionService;
    private final ConnectorRegistry connectorRegistry;
    private final ObjectMapper objectMapper;
    
    public SellerServiceImpl(
            SellerRepository sellerRepository,
            SellerWarehouseRepository warehouseRepository,
            SellerPlatformRepository platformRepository,
            AuthService authService,
            CredentialEncryptionService encryptionService,
            ConnectorRegistry connectorRegistry,
            ObjectMapper objectMapper) {
        this.sellerRepository = sellerRepository;
        this.warehouseRepository = warehouseRepository;
        this.platformRepository = platformRepository;
        this.authService = authService;
        this.encryptionService = encryptionService;
        this.connectorRegistry = connectorRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public SellerDto createSeller(CreateSellerRequest request) {
        User currentUser = authService.getCurrentUser();
        
        // Check if seller already exists for this user
        if (sellerRepository.findByUserId(currentUser.getId()).isPresent()) {
            throw new ValidationException("Seller profile already exists for this user");
        }
        
        Seller seller = new Seller();
        seller.setUser(currentUser);
        seller.setBusinessName(request.businessName());
        seller.setGstin(request.gstin());
        seller.setBusinessAddress(request.businessAddress());
        seller.setPhoneNumber(request.phoneNumber());
        seller.setStatus(SellerStatus.PENDING);
        
        seller = sellerRepository.save(seller);
        return toSellerDto(seller);
    }
    
    @Override
    @Transactional(readOnly = true)
    public SellerDto getSellerProfile() {
        Seller seller = getCurrentSeller();
        return toSellerDto(seller);
    }
    
    @Override
    public SellerDto updateSeller(Long sellerId, CreateSellerRequest request) {
        Seller seller = getCurrentSeller();
        
        if (!seller.getId().equals(sellerId)) {
            throw new ValidationException("Cannot update another seller's profile");
        }
        
        seller.setBusinessName(request.businessName());
        seller.setGstin(request.gstin());
        seller.setBusinessAddress(request.businessAddress());
        seller.setPhoneNumber(request.phoneNumber());
        
        seller = sellerRepository.save(seller);
        return toSellerDto(seller);
    }

    @Override
    public WarehouseDto addWarehouse(CreateWarehouseRequest request) {
        Seller seller = getCurrentSeller();
        
        // If this warehouse is marked as default, unmark any existing default
        if (request.isDefault()) {
            warehouseRepository.findBySellerIdAndIsDefaultTrue(seller.getId())
                    .ifPresent(existingDefault -> {
                        existingDefault.setDefault(false);
                        warehouseRepository.save(existingDefault);
                    });
        }
        
        SellerWarehouse warehouse = new SellerWarehouse();
        warehouse.setSeller(seller);
        warehouse.setName(request.name());
        warehouse.setAddress(request.address());
        warehouse.setCity(request.city());
        warehouse.setState(request.state());
        warehouse.setPincode(request.pincode());
        warehouse.setDefault(request.isDefault());
        
        warehouse = warehouseRepository.save(warehouse);
        return toWarehouseDto(warehouse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDto> getWarehouses() {
        Seller seller = getCurrentSeller();
        return warehouseRepository.findBySellerId(seller.getId())
                .stream()
                .map(this::toWarehouseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public WarehouseDto updateWarehouse(Long warehouseId, CreateWarehouseRequest request) {
        Seller seller = getCurrentSeller();
        SellerWarehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        
        if (!warehouse.getSeller().getId().equals(seller.getId())) {
            throw new ValidationException("Cannot update another seller's warehouse");
        }
        
        // If this warehouse is being marked as default, unmark any existing default
        if (request.isDefault() && !warehouse.isDefault()) {
            warehouseRepository.findBySellerIdAndIsDefaultTrue(seller.getId())
                    .ifPresent(existingDefault -> {
                        existingDefault.setDefault(false);
                        warehouseRepository.save(existingDefault);
                    });
        }
        
        warehouse.setName(request.name());
        warehouse.setAddress(request.address());
        warehouse.setCity(request.city());
        warehouse.setState(request.state());
        warehouse.setPincode(request.pincode());
        warehouse.setDefault(request.isDefault());
        
        warehouse = warehouseRepository.save(warehouse);
        return toWarehouseDto(warehouse);
    }

    @Override
    public void deleteWarehouse(Long warehouseId) {
        Seller seller = getCurrentSeller();
        SellerWarehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        
        if (!warehouse.getSeller().getId().equals(seller.getId())) {
            throw new ValidationException("Cannot delete another seller's warehouse");
        }
        
        // TODO: Verify no active inventory exists in this warehouse
        // This will be implemented when the Inventory module is complete
        
        warehouseRepository.delete(warehouse);
    }
    
    @Override
    public PlatformConnectionDto connectPlatform(ConnectPlatformRequest request) {
        Seller seller = getCurrentSeller();
        
        // Get the connector for this platform
        MarketplaceConnector connector = connectorRegistry.getConnector(request.platformType());
        
        // Test the connection
        ConnectionTestResult testResult = connector.testConnection(request.credentials());
        
        // Encrypt credentials
        String credentialsJson = toJson(request.credentials());
        String encryptedCredentials = encryptionService.encrypt(credentialsJson);
        
        // Create platform connection
        SellerPlatform platform = new SellerPlatform();
        platform.setSeller(seller);
        platform.setPlatformType(request.platformType());
        platform.setStoreName(request.storeName());
        platform.setEncryptedCredentials(encryptedCredentials);
        platform.setCreationMethod(AccountCreationMethod.EXISTING_ACCOUNT);
        
        if (testResult.success()) {
            platform.setStatus(ConnectionStatus.CONNECTED);
        } else {
            platform.setStatus(ConnectionStatus.ERROR);
            platform.setLastSyncError(testResult.message());
        }
        
        platform = platformRepository.save(platform);
        return toPlatformConnectionDto(platform);
    }

    @Override
    public PlatformConnectionDto createPlatformAccount(CreatePlatformAccountRequest request) {
        Seller seller = getCurrentSeller();
        
        // Get the connector for this platform
        MarketplaceConnector connector = connectorRegistry.getConnector(request.platformType());
        
        // Create platform connection with CREATING status
        SellerPlatform platform = new SellerPlatform();
        platform.setSeller(seller);
        platform.setPlatformType(request.platformType());
        platform.setStoreName(request.storeName());
        platform.setStatus(ConnectionStatus.CREATING);
        platform.setCreationMethod(AccountCreationMethod.AUTO_CREATED);
        platform.setEncryptedCredentials(""); // Temporary empty value
        
        platform = platformRepository.save(platform);
        
        try {
            // Prepare onboarding data
            MarketplaceOnboardingData onboardingData = prepareOnboardingData(seller, request.storeName());
            
            // Call connector to create account
            Map<String, String> credentials = connector.createSellerAccount(onboardingData);
            
            // Encrypt and store credentials
            String credentialsJson = toJson(credentials);
            String encryptedCredentials = encryptionService.encrypt(credentialsJson);
            platform.setEncryptedCredentials(encryptedCredentials);
            
            // Extract external store ID if present
            if (credentials.containsKey("store_id")) {
                platform.setExternalStoreId(credentials.get("store_id"));
            } else if (credentials.containsKey("seller_id")) {
                platform.setExternalStoreId(credentials.get("seller_id"));
            }
            
            // Store platform metadata
            if (credentials.containsKey("metadata")) {
                platform.setPlatformMetadata(credentials.get("metadata"));
            }
            
            platform.setStatus(ConnectionStatus.CONNECTED);
            platform.setLastSyncError(null);
            
        } catch (UnsupportedOperationException e) {
            platform.setStatus(ConnectionStatus.ERROR);
            platform.setLastSyncError("Platform does not support automatic account creation");
        } catch (Exception e) {
            platform.setStatus(ConnectionStatus.ERROR);
            platform.setLastSyncError("Account creation failed: " + e.getMessage());
        }
        
        platform = platformRepository.save(platform);
        return toPlatformConnectionDto(platform);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlatformConnectionDto> getConnectedPlatforms() {
        Seller seller = getCurrentSeller();
        return platformRepository.findBySellerId(seller.getId())
                .stream()
                .map(this::toPlatformConnectionDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public void disconnectPlatform(Long platformId) {
        Seller seller = getCurrentSeller();
        SellerPlatform platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new ResourceNotFoundException("Platform connection not found"));
        
        if (!platform.getSeller().getId().equals(seller.getId())) {
            throw new ValidationException("Cannot disconnect another seller's platform");
        }
        
        // TODO: Verify no active listings exist on this platform
        // This will be implemented when the Listing module is complete
        
        platformRepository.delete(platform);
    }
    
    @Override
    public PlatformConnectionDto testConnection(Long platformId) {
        Seller seller = getCurrentSeller();
        SellerPlatform platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new ResourceNotFoundException("Platform connection not found"));
        
        if (!platform.getSeller().getId().equals(seller.getId())) {
            throw new ValidationException("Cannot test another seller's platform connection");
        }
        
        // Get the connector
        MarketplaceConnector connector = connectorRegistry.getConnector(platform.getPlatformType());
        
        // Decrypt credentials
        String credentialsJson = encryptionService.decrypt(platform.getEncryptedCredentials());
        Map<String, String> credentials = fromJson(credentialsJson);
        
        // Test connection
        ConnectionTestResult testResult = connector.testConnection(credentials);
        
        if (testResult.success()) {
            platform.setStatus(ConnectionStatus.CONNECTED);
            platform.setLastSyncError(null);
        } else {
            platform.setStatus(ConnectionStatus.ERROR);
            platform.setLastSyncError(testResult.message());
        }
        
        platform = platformRepository.save(platform);
        return toPlatformConnectionDto(platform);
    }

    // Helper methods
    
    private Seller getCurrentSeller() {
        User currentUser = authService.getCurrentUser();
        return sellerRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
    }
    
    private MarketplaceOnboardingData prepareOnboardingData(Seller seller, String storeName) {
        User user = seller.getUser();
        
        // Get primary warehouse
        WarehouseDto primaryWarehouse = warehouseRepository.findBySellerIdAndIsDefaultTrue(seller.getId())
                .map(this::toWarehouseDto)
                .orElse(null);
        
        return new MarketplaceOnboardingData(
                seller.getBusinessName(),
                user.getFullName(),
                user.getEmail(),
                seller.getPhoneNumber(),
                seller.getGstin(),
                seller.getBusinessAddress(),
                primaryWarehouse != null ? primaryWarehouse.city() : "",
                primaryWarehouse != null ? primaryWarehouse.state() : "",
                primaryWarehouse != null ? primaryWarehouse.pincode() : "",
                null, // bankAccountNumber - not collected in MVP
                null, // bankIfscCode - not collected in MVP
                null, // bankAccountHolderName - not collected in MVP
                primaryWarehouse
        );
    }
    
    private SellerDto toSellerDto(Seller seller) {
        return new SellerDto(
                seller.getId(),
                seller.getBusinessName(),
                seller.getGstin(),
                seller.getBusinessAddress(),
                seller.getPhoneNumber(),
                seller.getStatus(),
                seller.getCreatedAt()
        );
    }
    
    private WarehouseDto toWarehouseDto(SellerWarehouse warehouse) {
        return new WarehouseDto(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getCity(),
                warehouse.getState(),
                warehouse.getPincode(),
                warehouse.isDefault()
        );
    }

    private PlatformConnectionDto toPlatformConnectionDto(SellerPlatform platform) {
        return new PlatformConnectionDto(
                platform.getId(),
                platform.getPlatformType(),
                platform.getStoreName(),
                platform.getStatus(),
                platform.getCreationMethod(),
                platform.getExternalStoreId(),
                platform.getLastSyncedAt(),
                platform.getLastSyncError()
        );
    }
    
    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize credentials", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize credentials", e);
        }
    }
}

