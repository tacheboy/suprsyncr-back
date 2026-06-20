package com.suprsyncr.analytics.repository;

import com.suprsyncr.analytics.domain.DummyProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DummyProductRepository extends JpaRepository<DummyProduct, String> {
    List<DummyProduct> findByStoreId(String storeId);
}

