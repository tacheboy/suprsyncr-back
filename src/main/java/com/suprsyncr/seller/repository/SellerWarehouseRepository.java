package com.suprsyncr.seller.repository;

import com.suprsyncr.seller.entity.SellerWarehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SellerWarehouse entity.
 */
@Repository
public interface SellerWarehouseRepository extends JpaRepository<SellerWarehouse, Long> {
    
    /**
     * Finds all warehouses for a seller.
     * 
     * @param sellerId the seller ID
     * @return list of warehouses
     */
    List<SellerWarehouse> findBySellerId(Long sellerId);
    
    /**
     * Finds the default warehouse for a seller.
     * 
     * @param sellerId the seller ID
     * @return Optional containing the default warehouse if found
     */
    Optional<SellerWarehouse> findBySellerIdAndIsDefaultTrue(Long sellerId);
}

