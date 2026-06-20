package com.suprsyncr.seller.service;

import com.suprsyncr.seller.dto.*;

import java.util.List;

/**
 * Service interface for seller operations.
 */
public interface SellerService {
    
    /**
     * Creates a new seller profile for the current user.
     * 
     * @param request the seller creation request
     * @return the created seller DTO
     */
    SellerDto createSeller(CreateSellerRequest request);
    
    /**
     * Gets the seller profile for the current user.
     * 
     * @return the seller DTO
     */
    SellerDto getSellerProfile();
    
    /**
     * Updates a seller profile.
     * 
     * @param sellerId the seller ID
     * @param request the update request
     * @return the updated seller DTO
     */
    SellerDto updateSeller(Long sellerId, CreateSellerRequest request);
    
    /**
     * Adds a warehouse for the current seller.
     * 
     * @param request the warehouse creation request
     * @return the created warehouse DTO
     */
    WarehouseDto addWarehouse(CreateWarehouseRequest request);
    
    /**
     * Gets all warehouses for the current seller.
     * 
     * @return list of warehouse DTOs
     */
    List<WarehouseDto> getWarehouses();
    
    /**
     * Updates a warehouse.
     * 
     * @param warehouseId the warehouse ID
     * @param request the update request
     * @return the updated warehouse DTO
     */
    WarehouseDto updateWarehouse(Long warehouseId, CreateWarehouseRequest request);
    
    /**
     * Deletes a warehouse.
     * 
     * @param warehouseId the warehouse ID
     */
    void deleteWarehouse(Long warehouseId);
    
    /**
     * Connects an existing platform account.
     * 
     * @param request the platform connection request
     * @return the platform connection DTO
     */
    PlatformConnectionDto connectPlatform(ConnectPlatformRequest request);
    
    /**
     * Creates a new platform account automatically.
     * 
     * @param request the platform account creation request
     * @return the platform connection DTO
     */
    PlatformConnectionDto createPlatformAccount(CreatePlatformAccountRequest request);
    
    /**
     * Gets all connected platforms for the current seller.
     * 
     * @return list of platform connection DTOs
     */
    List<PlatformConnectionDto> getConnectedPlatforms();
    
    /**
     * Disconnects a platform.
     * 
     * @param platformId the platform ID
     */
    void disconnectPlatform(Long platformId);
    
    /**
     * Tests a platform connection.
     * 
     * @param platformId the platform ID
     * @return the platform connection DTO with updated status
     */
    PlatformConnectionDto testConnection(Long platformId);
}

