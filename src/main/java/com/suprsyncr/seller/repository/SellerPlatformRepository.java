package com.suprsyncr.seller.repository;

import com.suprsyncr.seller.entity.ConnectionStatus;
import com.suprsyncr.seller.entity.SellerPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SellerPlatform entity.
 */
@Repository
public interface SellerPlatformRepository extends JpaRepository<SellerPlatform, Long> {
    
    /**
     * Finds all platform connections for a seller.
     * 
     * @param sellerId the seller ID
     * @return list of platform connections
     */
    List<SellerPlatform> findBySellerId(Long sellerId);
    
    /**
     * Finds all platform connections with a specific status.
     * 
     * @param status the connection status
     * @return list of platform connections
     */
    List<SellerPlatform> findByStatus(ConnectionStatus status);
}

