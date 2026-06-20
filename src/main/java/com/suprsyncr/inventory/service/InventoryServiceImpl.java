package com.suprsyncr.inventory.service;

import com.suprsyncr.common.exception.InsufficientStockException;
import com.suprsyncr.common.exception.ResourceNotFoundException;
import com.suprsyncr.inventory.dto.InventoryDto;
import com.suprsyncr.inventory.dto.InventoryTransactionDto;
import com.suprsyncr.inventory.dto.ReserveInventoryRequest;
import com.suprsyncr.inventory.dto.UpdateInventoryRequest;
import com.suprsyncr.inventory.entity.Inventory;
import com.suprsyncr.inventory.entity.InventoryTransaction;
import com.suprsyncr.inventory.entity.TransactionType;
import com.suprsyncr.inventory.repository.InventoryRepository;
import com.suprsyncr.inventory.repository.InventoryTransactionRepository;
import com.suprsyncr.product.entity.ProductVariant;
import com.suprsyncr.product.repository.ProductVariantRepository;
import com.suprsyncr.seller.entity.SellerWarehouse;
import com.suprsyncr.seller.repository.SellerWarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of InventoryService with atomic operations.
 * Uses pessimistic locking for reservation operations to prevent race conditions.
 * 
 * Requirements: 9, 10, 11, 55, 56, 82, 83, 95
 */
@Service
public class InventoryServiceImpl implements InventoryService {
    
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductVariantRepository variantRepository;
    private final SellerWarehouseRepository warehouseRepository;
    
    public InventoryServiceImpl(
            InventoryRepository inventoryRepository,
            InventoryTransactionRepository transactionRepository,
            ProductVariantRepository variantRepository,
            SellerWarehouseRepository warehouseRepository) {
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.variantRepository = variantRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Override
    public InventoryDto getInventory(Long productVariantId, Long warehouseId) {
        Inventory inventory = inventoryRepository.findByProductVariantIdAndWarehouseId(productVariantId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for variant " + productVariantId + " at warehouse " + warehouseId));
        return toDto(inventory);
    }
    
    @Override
    public List<InventoryDto> getInventoryByProduct(Long productId) {
        return inventoryRepository.findByProductVariantProductId(productId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<InventoryDto> getInventoryByWarehouse(Long warehouseId) {
        return inventoryRepository.findByWarehouseId(warehouseId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<InventoryDto> getLowStockItems() {
        return inventoryRepository.findLowStockInventory().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryDto updateInventory(UpdateInventoryRequest request) {
        Inventory inventory = getOrCreateInventory(request.productVariantId(), request.warehouseId());
        
        // Set available quantity to the requested amount
        inventory.setAvailableQuantity(request.quantity());
        // Update total = available + reserved
        inventory.setTotalQuantity(inventory.getAvailableQuantity() + inventory.getReservedQuantity());
        
        inventory = inventoryRepository.save(inventory);
        
        // Create transaction record
        createTransaction(inventory, TransactionType.ADJUSTMENT, request.quantity(), 
                inventory.getAvailableQuantity(), "MANUAL", null, request.notes());
        
        return toDto(inventory);
    }
    
    @Override
    @Transactional
    public InventoryDto adjustInventory(Long productVariantId, Long warehouseId, Integer adjustment, String notes) {
        Inventory inventory = getOrCreateInventory(productVariantId, warehouseId);
        
        // Adjust available quantity by positive or negative amount
        int newAvailable = inventory.getAvailableQuantity() + adjustment;
        if (newAvailable < 0) {
            throw new IllegalArgumentException("Adjustment would result in negative available quantity");
        }
        
        inventory.setAvailableQuantity(newAvailable);
        // Update total = available + reserved
        inventory.setTotalQuantity(inventory.getAvailableQuantity() + inventory.getReservedQuantity());
        
        inventory = inventoryRepository.save(inventory);
        
        // Create transaction record
        createTransaction(inventory, TransactionType.ADJUSTMENT, adjustment, 
                inventory.getAvailableQuantity(), "MANUAL", null, notes);
        
        return toDto(inventory);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void reserveInventory(ReserveInventoryRequest request) {
        // Use pessimistic locking (SELECT FOR UPDATE) to prevent race conditions
        Inventory inventory = inventoryRepository
                .findByProductVariantIdAndWarehouseIdWithLock(request.productVariantId(), request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for variant " + request.productVariantId() + 
                        " at warehouse " + request.warehouseId()));
        
        // Verify available >= requested
        if (inventory.getAvailableQuantity() < request.quantity()) {
            throw new InsufficientStockException(
                    request.productVariantId(), 
                    request.quantity(), 
                    inventory.getAvailableQuantity());
        }
        
        // Decrease available, increase reserved
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - request.quantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
        // Maintain invariant: total = available + reserved (total stays the same)
        
        inventoryRepository.save(inventory);
        
        // Create RESERVE transaction
        createTransaction(inventory, TransactionType.RESERVE, request.quantity(), 
                inventory.getAvailableQuantity(), request.referenceType(), request.referenceId(), 
                "Reserved " + request.quantity() + " units");
    }

    @Override
    @Transactional
    public void releaseInventory(Long productVariantId, Long warehouseId, Integer quantity, 
                                  String referenceType, String referenceId) {
        Inventory inventory = inventoryRepository
                .findByProductVariantIdAndWarehouseId(productVariantId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for variant " + productVariantId + " at warehouse " + warehouseId));
        
        // Increase available, decrease reserved
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);
        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        // Maintain invariant: total = available + reserved (total stays the same)
        
        inventoryRepository.save(inventory);
        
        // Create RELEASE transaction
        createTransaction(inventory, TransactionType.RELEASE, quantity, 
                inventory.getAvailableQuantity(), referenceType, referenceId, 
                "Released " + quantity + " units");
    }
    
    @Override
    @Transactional
    public void commitReservation(Long productVariantId, Long warehouseId, Integer quantity, 
                                   String referenceType, String referenceId) {
        Inventory inventory = inventoryRepository
                .findByProductVariantIdAndWarehouseId(productVariantId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for variant " + productVariantId + " at warehouse " + warehouseId));
        
        // Decrease reserved (stock out)
        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        // Decrease total as well since stock is leaving
        inventory.setTotalQuantity(inventory.getAvailableQuantity() + inventory.getReservedQuantity());
        
        inventoryRepository.save(inventory);
        
        // Create STOCK_OUT transaction
        createTransaction(inventory, TransactionType.STOCK_OUT, quantity, 
                inventory.getTotalQuantity(), referenceType, referenceId, 
                "Committed reservation of " + quantity + " units");
    }

    @Override
    public List<InventoryTransactionDto> getTransactionHistory(Long inventoryId, int page, int size) {
        Page<InventoryTransaction> transactions = transactionRepository
                .findByInventoryIdOrderByCreatedAtDesc(inventoryId, PageRequest.of(page, size));
        
        return transactions.getContent().stream()
                .map(this::toTransactionDto)
                .collect(Collectors.toList());
    }
    
    // Helper methods
    
    private Inventory getOrCreateInventory(Long productVariantId, Long warehouseId) {
        return inventoryRepository.findByProductVariantIdAndWarehouseId(productVariantId, warehouseId)
                .orElseGet(() -> {
                    ProductVariant variant = variantRepository.findById(productVariantId)
                            .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + productVariantId));
                    SellerWarehouse warehouse = warehouseRepository.findById(warehouseId)
                            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
                    
                    Inventory inventory = new Inventory();
                    inventory.setProductVariant(variant);
                    inventory.setWarehouse(warehouse);
                    inventory.setAvailableQuantity(0);
                    inventory.setReservedQuantity(0);
                    inventory.setTotalQuantity(0);
                    inventory.setLowStockThreshold(10);
                    
                    return inventoryRepository.save(inventory);
                });
    }

    private void createTransaction(Inventory inventory, TransactionType type, Integer quantity, 
                                    Integer balanceAfter, String referenceType, String referenceId, String notes) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventory(inventory);
        transaction.setType(type);
        transaction.setQuantity(quantity);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transaction.setNotes(notes);
        
        transactionRepository.save(transaction);
    }
    
    private InventoryDto toDto(Inventory inventory) {
        boolean isLowStock = inventory.getAvailableQuantity() < inventory.getLowStockThreshold();
        
        return new InventoryDto(
                inventory.getId(),
                inventory.getProductVariant().getId(),
                inventory.getProductVariant().getSku(),
                inventory.getWarehouse().getId(),
                inventory.getWarehouse().getName(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getTotalQuantity(),
                inventory.getLowStockThreshold(),
                isLowStock
        );
    }
    
    private InventoryTransactionDto toTransactionDto(InventoryTransaction transaction) {
        return new InventoryTransactionDto(
                transaction.getId(),
                transaction.getType(),
                transaction.getQuantity(),
                transaction.getBalanceAfter(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getNotes(),
                transaction.getCreatedAt()
        );
    }
}

