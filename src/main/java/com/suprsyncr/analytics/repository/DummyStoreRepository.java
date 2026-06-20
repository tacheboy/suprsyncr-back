package com.suprsyncr.analytics.repository;

import com.suprsyncr.analytics.domain.DummyStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DummyStoreRepository extends JpaRepository<DummyStore, String> {
}

