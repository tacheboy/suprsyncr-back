package com.suprsyncr.seller.repository;

import com.suprsyncr.seller.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Seller entity.
 */
@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    
    /**
     * Finds a seller by user ID.
     * 
     * @param userId the user ID
     * @return Optional containing the seller if found
     */
    Optional<Seller> findByUserId(Long userId);
}

