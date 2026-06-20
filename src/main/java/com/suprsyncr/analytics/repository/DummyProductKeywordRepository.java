package com.suprsyncr.analytics.repository;

import com.suprsyncr.analytics.domain.DummyProductKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DummyProductKeywordRepository extends JpaRepository<DummyProductKeyword, Long> {
    List<DummyProductKeyword> findByProductId(String productId);
    List<DummyProductKeyword> findByProductIdIn(List<String> productIds);
}

