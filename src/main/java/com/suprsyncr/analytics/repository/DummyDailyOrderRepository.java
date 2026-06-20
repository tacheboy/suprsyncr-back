package com.suprsyncr.analytics.repository;

import com.suprsyncr.analytics.domain.DummyDailyOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DummyDailyOrderRepository extends JpaRepository<DummyDailyOrder, String> {
    List<DummyDailyOrder> findByStoreIdAndOrderDateBetween(String storeId, LocalDate from, LocalDate to);
    List<DummyDailyOrder> findByStoreId(String storeId);
}

