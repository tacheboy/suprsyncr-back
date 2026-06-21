package com.suprsyncr.product.studio.repository;

import com.suprsyncr.product.studio.entity.ProductDraft;
import com.suprsyncr.product.studio.entity.ProductDraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductDraftRepository extends JpaRepository<ProductDraft, UUID> {

    List<ProductDraft> findByStoreIdOrderByCreatedAtDesc(String storeId);

    List<ProductDraft> findByStoreIdAndStatusOrderByCreatedAtDesc(String storeId,
                                                                 ProductDraftStatus status);
}
