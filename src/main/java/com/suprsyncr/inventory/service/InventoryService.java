package com.suprsyncr.inventory.service;

import com.suprsyncr.inventory.dto.InventoryDto;
import com.suprsyncr.inventory.dto.InventoryTransactionDto;
import com.suprsyncr.inventory.dto.ReserveInventoryRequest;
import com.suprsyncr.inventory.dto.UpdateInventoryRequest;

import java.util.List;

/**
 * Service interface for inventory management operations.
 * Provides atomic operations for inventory tracking and reservation.
 * 
 * Requirements: 9, 10, 11, 55, 56, 82, 83, 95
 */
public interface InventoryService {
    
    /**
     * Get inventory for a specific product variant at a warehouse.
     */
    InventoryDto getInventory(Long productVariantId, Long warehouseId);
    
    /**
     * Get all inventory records for a product across all warehouses.
     */
    List<InventoryDto> getInventoryByProduct(Long productId);
    
    /**
     * Get all inventory records for a specific warehouse.
     */
    List<InventoryDto> getInventoryByWarehouse(Long warehouseId);
    
    /**
     * Get all inventory records where available quantity is below low stock threshold.
     */
    List<InventoryDto> getLowStockItems();
    
    /**
     * Update inventory to a specific quantity.
     */
    InventoryDto updateInventory(UpdateInventoryRequest request);
    
    /**
     * Adjust inventory by a positive or negative amount.
     */
    InventoryDto adjustInventory(Long productVariantId, Long warehouseId, Integer adjustment, String notes);
    
    /**
     * Reserve inventory atomically with pessimistic locking.
     * Throws InsufficientStockException if not enough stock available.
     */
    void reserveInventory(ReserveInventoryRequest request);
    
    /**
     * Release reserved inventory back to available stock.
     */
    void releaseInventory(Long productVariantId, Long warehouseId, Integer quantity, String referenceType, String referenceId);
    
    /**
     * Commit a reservation by decreasing reserved quantity (stock out).
     */
    void commitReservation(Long productVariantId, Long warehouseId, Integer quantity, String referenceType, String referenceId);
    
    /**
     * Get transaction history for an inventory record.
     */
    List<InventoryTransactionDto> getTransactionHistory(Long inventoryId, int page, int size);
}

