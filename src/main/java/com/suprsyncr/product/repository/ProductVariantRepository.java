package com.suprsyncr.product.repository;

import com.suprsyncr.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ProductVariant entity.
 */
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findByProductIdAndSku(Long productId, String sku);

    /**
     * Find variants for a product identified by its SKU within a seller's catalogue.
     * Used by order ingestion to link a Shopify order item (externalProductId →
     * sku "shopify-{id}") to the matching local ProductVariant so attribution
     * can trace the order back to the right product.
     */
    @Query("""
        SELECT v FROM ProductVariant v
        WHERE v.product.seller.id = :sellerId
          AND v.product.sku = :productSku
        ORDER BY v.id ASC
        """)
    List<ProductVariant> findByProductSellerIdAndProductSku(
            @Param("sellerId") Long sellerId,
            @Param("productSku") String productSku);
}

