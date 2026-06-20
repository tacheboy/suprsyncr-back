package com.suprsyncr.inventory.repository;

import com.suprsyncr.inventory.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for InventoryTransaction entity.
 * Provides audit trail access for inventory changes.
 * 
 * Requirements: 9, 10, 11, 55, 94
 */
@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    
    /**
     * Find all transactions for a specific inventory record, ordered by creation time descending.
     * Returns paginated results for efficient retrieval of transaction history.
     * 
     * @param inventoryId the inventory ID
     * @param pageable pagination parameters
     * @return page of inventory transactions
     */
    Page<InventoryTransaction> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId, Pageable pageable);
}

