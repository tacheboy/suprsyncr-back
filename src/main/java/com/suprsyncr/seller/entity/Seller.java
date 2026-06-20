package com.suprsyncr.seller.entity;

import com.suprsyncr.auth.entity.User;
import com.suprsyncr.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a seller business profile.
 */
@Entity
@Table(name = "sellers")
public class Seller extends BaseEntity {
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(nullable = false)
    private String businessName;
    
    @Column(length = 15)
    private String gstin;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String businessAddress;
    
    @Column(nullable = false, length = 20)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SellerStatus status;
    
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SellerWarehouse> warehouses = new ArrayList<>();
    
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SellerPlatform> platforms = new ArrayList<>();
    
    // Getters and Setters
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getBusinessName() {
        return businessName;
    }
    
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }
    
    public String getGstin() {
        return gstin;
    }
    
    public void setGstin(String gstin) {
        this.gstin = gstin;
    }
    
    public String getBusinessAddress() {
        return businessAddress;
    }
    
    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public SellerStatus getStatus() {
        return status;
    }
    
    public void setStatus(SellerStatus status) {
        this.status = status;
    }
    
    public List<SellerWarehouse> getWarehouses() {
        return warehouses;
    }
    
    public void setWarehouses(List<SellerWarehouse> warehouses) {
        this.warehouses = warehouses;
    }
    
    public List<SellerPlatform> getPlatforms() {
        return platforms;
    }
    
    public void setPlatforms(List<SellerPlatform> platforms) {
        this.platforms = platforms;
    }
}

