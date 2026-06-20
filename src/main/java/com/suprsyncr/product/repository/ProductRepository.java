package com.suprsyncr.product.repository;

import com.suprsyncr.product.entity.Product;
import com.suprsyncr.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Product entity.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Original JPQL query that caused lower(bytea) error in PostgreSQL:
    // @Query("SELECT p FROM Product p WHERE p.seller.id = :sellerId " +
    //        "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
    //        "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
    //        "AND (:status IS NULL OR p.status = :status)")
    // Page<Product> findProducts(
    //     @Param("sellerId") Long sellerId,
    //     @Param("search") String search,
    //     @Param("categoryId") Long categoryId,
    //     @Param("status") ProductStatus status,
    //     Pageable pageable
    // );
    
    Optional<Product> findBySellerIdAndSku(Long sellerId, String sku);
    
    @Query(value = """
        SELECT * FROM products p
        WHERE p.seller_id = :sellerId
        AND (:search IS NULL OR p.name ILIKE '%' || CAST(:search AS TEXT) || '%')
        AND (:categoryId IS NULL OR p.category_id = :categoryId)
        AND (CAST(:status AS TEXT) IS NULL OR p.status = CAST(:status AS TEXT))
        ORDER BY p.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM products p
        WHERE p.seller_id = :sellerId
        AND (:search IS NULL OR p.name ILIKE '%' || CAST(:search AS TEXT) || '%')
        AND (:categoryId IS NULL OR p.category_id = :categoryId)
        AND (CAST(:status AS TEXT) IS NULL OR p.status = CAST(:status AS TEXT))
        """,
        nativeQuery = true)
    Page<Product> findProducts(
        @Param("sellerId") Long sellerId,
        @Param("search") String search,
        @Param("categoryId") Long categoryId,
        @Param("status") String status,
        Pageable pageable
    );
}

