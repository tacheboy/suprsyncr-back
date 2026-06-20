package com.suprsyncr.inventory.repository;

import com.suprsyncr.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Inventory entity.
 * Provides atomic operations for inventory tracking with pessimistic locking support.
 * 
 * Requirements: 9, 10, 11, 55, 94
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    /**
     * Find inventory by product variant and warehouse.
     * 
     * @param variantId the product variant ID
     * @param warehouseId the warehouse ID
     * @return Optional containing the inventory if found
     */
    Optional<Inventory> findByProductVariantIdAndWarehouseId(Long variantId, Long warehouseId);
    
    /**
     * Find all inventory records for a product across all warehouses.
     * 
     * @param productId the product ID
     * @return list of inventory records
     */
    List<Inventory> findByProductVariantProductId(Long productId);
    
    /**
     * Find all inventory records for a specific warehouse.
     * 
     * @param warehouseId the warehouse ID
     * @return list of inventory records
     */
    List<Inventory> findByWarehouseId(Long warehouseId);
    
    /**
     * Find all inventory records where available quantity is below the low stock threshold.
     * This helps identify products that need restocking.
     * 
     * @return list of low stock inventory records
     */
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity < i.lowStockThreshold")
    List<Inventory> findLowStockInventory();
    
    /**
     * Find and lock inventory for atomic reservation operations.
     * Uses pessimistic write lock to prevent race conditions during inventory reservations.
     * This is critical for preventing overselling when multiple concurrent orders
     * attempt to reserve the same inventory.
     * 
     * @param variantId the product variant ID
     * @param warehouseId the warehouse ID
     * @return Optional containing the locked inventory if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productVariant.id = :variantId AND i.warehouse.id = :warehouseId")
    Optional<Inventory> findByProductVariantIdAndWarehouseIdWithLock(@Param("variantId") Long variantId, @Param("warehouseId") Long warehouseId);
}

