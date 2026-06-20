package com.suprsyncr.analytics.repository;

import com.suprsyncr.analytics.domain.DummySessionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DummySessionEventRepository extends JpaRepository<DummySessionEvent, Long> {
    List<DummySessionEvent> findByStoreIdAndEventDateBetween(String storeId, LocalDate from, LocalDate to);
    List<DummySessionEvent> findByStoreId(String storeId);
}

