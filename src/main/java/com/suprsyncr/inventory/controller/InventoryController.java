package com.suprsyncr.inventory.controller;

import com.suprsyncr.common.dto.ApiResponse;
import com.suprsyncr.inventory.dto.InventoryDto;
import com.suprsyncr.inventory.dto.InventoryTransactionDto;
import com.suprsyncr.inventory.dto.UpdateInventoryRequest;
import com.suprsyncr.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for inventory management.
 * Provides endpoints for tracking and managing stock levels across warehouses.
 * 
 * Requirements: 9, 11, 55, 56, 96
 */
@RestController
@RequestMapping("/api/v1/inventory")
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Inventory", description = "Inventory tracking and stock management endpoints")
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    /**
     * Get inventory for a specific product variant at a warehouse.
     * 
     * @param variantId Product variant ID
     * @param warehouseId Warehouse ID
     * @return Inventory details
     */
    @GetMapping("/variant/{variantId}/warehouse/{warehouseId}")
    @Operation(summary = "Get inventory", description = "Retrieves inventory details for a specific product variant at a warehouse")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Inventory not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<InventoryDto>> getInventory(
        @PathVariable Long variantId,
        @PathVariable Long warehouseId
    ) {
        InventoryDto inventory = inventoryService.getInventory(variantId, warehouseId);
        ApiResponse<InventoryDto> response = new ApiResponse<>(
            true,
            inventory,
            "Inventory retrieved successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all inventory records for a product across all warehouses.
     * 
     * @param productId Product ID
     * @return List of inventory records
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "Get inventory by product", description = "Retrieves all inventory records for a product across all warehouses")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<InventoryDto>>> getInventoryByProduct(
        @PathVariable Long productId
    ) {
        List<InventoryDto> inventoryList = inventoryService.getInventoryByProduct(productId);
        ApiResponse<List<InventoryDto>> response = new ApiResponse<>(
            true,
            inventoryList,
            "Inventory retrieved successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all inventory records for a specific warehouse.
     * 
     * @param warehouseId Warehouse ID
     * @return List of inventory records
     */
    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get inventory by warehouse", description = "Retrieves all inventory records for a specific warehouse")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<InventoryDto>>> getInventoryByWarehouse(
        @PathVariable Long warehouseId
    ) {
        List<InventoryDto> inventoryList = inventoryService.getInventoryByWarehouse(warehouseId);
        ApiResponse<List<InventoryDto>> response = new ApiResponse<>(
            true,
            inventoryList,
            "Inventory retrieved successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all inventory records where available quantity is below low stock threshold.
     * 
     * @return List of low stock inventory records
     */
    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock items", description = "Retrieves inventory records where available quantity is below the low stock threshold")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Low stock items retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<InventoryDto>>> getLowStockItems() {
        List<InventoryDto> lowStockItems = inventoryService.getLowStockItems();
        ApiResponse<List<InventoryDto>> response = new ApiResponse<>(
            true,
            lowStockItems,
            "Low stock items retrieved successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update inventory to a specific quantity.
     * 
     * @param request Update inventory request
     * @return Updated inventory details
     */
    @PutMapping
    @Operation(summary = "Update inventory", description = "Sets inventory to a specific quantity")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or negative quantity"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Inventory not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<InventoryDto>> updateInventory(
        @Valid @RequestBody UpdateInventoryRequest request
    ) {
        InventoryDto inventory = inventoryService.updateInventory(request);
        ApiResponse<InventoryDto> response = new ApiResponse<>(
            true,
            inventory,
            "Inventory updated successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Adjust inventory by a positive or negative amount.
     * 
     * @param variantId Product variant ID
     * @param warehouseId Warehouse ID
     * @param adjustment Adjustment amount (positive or negative)
     * @param notes Optional notes for the adjustment
     * @return Updated inventory details
     */
    @PostMapping("/adjust")
    @Operation(summary = "Adjust inventory", description = "Adjusts inventory by a positive or negative amount")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory adjusted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid adjustment amount"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Inventory not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<InventoryDto>> adjustInventory(
        @RequestParam Long variantId,
        @RequestParam Long warehouseId,
        @RequestParam Integer adjustment,
        @RequestParam(required = false) String notes
    ) {
        InventoryDto inventory = inventoryService.adjustInventory(variantId, warehouseId, adjustment, notes);
        ApiResponse<InventoryDto> response = new ApiResponse<>(
            true,
            inventory,
            "Inventory adjusted successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get transaction history for an inventory record with pagination.
     * 
     * @param inventoryId Inventory ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 50)
     * @return List of inventory transactions
     */
    @GetMapping("/{inventoryId}/transactions")
    @Operation(summary = "Get transaction history", description = "Retrieves paginated transaction history for an inventory record")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Inventory not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<InventoryTransactionDto>>> getTransactionHistory(
        @PathVariable Long inventoryId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        List<InventoryTransactionDto> transactions = inventoryService.getTransactionHistory(inventoryId, page, size);
        ApiResponse<List<InventoryTransactionDto>> response = new ApiResponse<>(
            true,
            transactions,
            "Transaction history retrieved successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
}

