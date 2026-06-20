package com.suprsyncr.seller.controller;

import com.suprsyncr.common.dto.ApiResponse;
import com.suprsyncr.seller.dto.*;
import com.suprsyncr.seller.service.SellerService;
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
 * REST controller for seller operations.
 */
@RestController
@RequestMapping("/api/v1/seller")
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller", description = "Seller profile, warehouse, and platform connection management endpoints")
public class SellerController {
    
    private final SellerService sellerService;
    
    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }
    
    @PostMapping
    @Operation(summary = "Create seller profile", description = "Creates a new seller profile with business information")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Seller profile created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<SellerDto>> createSeller(@Valid @RequestBody CreateSellerRequest request) {
        SellerDto seller = sellerService.createSeller(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, seller, "Seller profile created successfully", LocalDateTime.now()));
    }
    
    @GetMapping("/profile")
    @Operation(summary = "Get seller profile", description = "Retrieves the current seller's profile information")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller profile retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller profile not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<SellerDto>> getProfile() {
        SellerDto seller = sellerService.getSellerProfile();
        return ResponseEntity.ok(new ApiResponse<>(true, seller, "Seller profile retrieved successfully", LocalDateTime.now()));
    }
    
    @PutMapping("/{sellerId}")
    @Operation(summary = "Update seller profile", description = "Updates an existing seller profile with new business information")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller profile updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<SellerDto>> updateSeller(
            @PathVariable Long sellerId,
            @Valid @RequestBody CreateSellerRequest request) {
        SellerDto seller = sellerService.updateSeller(sellerId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, seller, "Seller profile updated successfully", LocalDateTime.now()));
    }

    @PostMapping("/warehouses")
    @Operation(summary = "Add warehouse", description = "Adds a new warehouse to the seller's fulfillment locations")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Warehouse added successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<WarehouseDto>> addWarehouse(@Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseDto warehouse = sellerService.addWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, warehouse, "Warehouse added successfully", LocalDateTime.now()));
    }
    
    @GetMapping("/warehouses")
    @Operation(summary = "Get warehouses", description = "Retrieves all warehouses for the current seller")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Warehouses retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<WarehouseDto>>> getWarehouses() {
        List<WarehouseDto> warehouses = sellerService.getWarehouses();
        return ResponseEntity.ok(new ApiResponse<>(true, warehouses, "Warehouses retrieved successfully", LocalDateTime.now()));
    }
    
    @PutMapping("/warehouses/{warehouseId}")
    @Operation(summary = "Update warehouse", description = "Updates an existing warehouse's information")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Warehouse updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<WarehouseDto>> updateWarehouse(
            @PathVariable Long warehouseId,
            @Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseDto warehouse = sellerService.updateWarehouse(warehouseId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, warehouse, "Warehouse updated successfully", LocalDateTime.now()));
    }
    
    @DeleteMapping("/warehouses/{warehouseId}")
    @Operation(summary = "Delete warehouse", description = "Deletes a warehouse if no active inventory exists")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Warehouse deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cannot delete warehouse with active inventory"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Void>> deleteWarehouse(@PathVariable Long warehouseId) {
        sellerService.deleteWarehouse(warehouseId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Warehouse deleted successfully", LocalDateTime.now()));
    }

    @PostMapping("/platforms")
    @Operation(summary = "Connect platform", description = "Connects an existing marketplace platform account using credentials")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Platform connected successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Platform connection test failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<PlatformConnectionDto>> connectPlatform(
            @Valid @RequestBody ConnectPlatformRequest request) {
        PlatformConnectionDto platform = sellerService.connectPlatform(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, platform, "Platform connected successfully", LocalDateTime.now()));
    }
    
    @PostMapping("/platforms/create")
    @Operation(summary = "Create platform account", description = "Initiates automatic creation of a new marketplace account")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Platform account creation initiated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Platform account creation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<PlatformConnectionDto>> createPlatformAccount(
            @Valid @RequestBody CreatePlatformAccountRequest request) {
        PlatformConnectionDto platform = sellerService.createPlatformAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, platform, "Platform account creation initiated", LocalDateTime.now()));
    }
    
    @GetMapping("/platforms")
    @Operation(summary = "Get connected platforms", description = "Retrieves all marketplace platform connections for the current seller")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Platform connections retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<PlatformConnectionDto>>> getConnectedPlatforms() {
        List<PlatformConnectionDto> platforms = sellerService.getConnectedPlatforms();
        return ResponseEntity.ok(new ApiResponse<>(true, platforms, "Platform connections retrieved successfully", LocalDateTime.now()));
    }
    
    @DeleteMapping("/platforms/{platformId}")
    @Operation(summary = "Disconnect platform", description = "Disconnects a marketplace platform if no active listings exist")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Platform disconnected successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Platform connection not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cannot disconnect platform with active listings"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Void>> disconnectPlatform(@PathVariable Long platformId) {
        sellerService.disconnectPlatform(platformId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Platform disconnected successfully", LocalDateTime.now()));
    }
    
    @PostMapping("/platforms/{platformId}/test")
    @Operation(summary = "Test platform connection", description = "Tests the connection to a marketplace platform")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Connection test completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Platform connection not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Connection test failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<PlatformConnectionDto>> testConnection(@PathVariable Long platformId) {
        PlatformConnectionDto platform = sellerService.testConnection(platformId);
        return ResponseEntity.ok(new ApiResponse<>(true, platform, "Connection test completed", LocalDateTime.now()));
    }
}

