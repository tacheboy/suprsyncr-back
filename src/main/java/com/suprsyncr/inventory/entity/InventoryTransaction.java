package com.suprsyncr.inventory.entity;

import com.suprsyncr.common.entity.BaseEntity;
import jakarta.persistence.*;

/**
 * Entity representing an audit record of inventory changes.
 * Provides complete traceability of all stock movements.
 * 
 * Requirements: 9, 10, 11, 56, 71, 82, 90, 91
 */
@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionType type;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private Integer balanceAfter;
    
    @Column(length = 50)
    private String referenceType;
    
    @Column(length = 255)
    private String referenceId;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    // Getters and Setters
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public void setType(TransactionType type) {
        this.type = type;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Integer getBalanceAfter() {
        return balanceAfter;
    }
    
    public void setBalanceAfter(Integer balanceAfter) {
        this.balanceAfter = balanceAfter;
    }
    
    public String getReferenceType() {
        return referenceType;
    }
    
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
    
    public String getReferenceId() {
        return referenceId;
    }
    
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}

