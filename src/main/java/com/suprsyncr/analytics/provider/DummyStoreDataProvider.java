package com.suprsyncr.analytics.provider;

import com.suprsyncr.analytics.domain.DummyDailyOrder;
import com.suprsyncr.analytics.domain.DummyProduct;
import com.suprsyncr.analytics.domain.DummyProductKeyword;
import com.suprsyncr.analytics.domain.DummySessionEvent;
import com.suprsyncr.analytics.domain.DummyStore;
import com.suprsyncr.analytics.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * StoreDataProvider implementation backed by the dummy_* tables in PostgreSQL.
 * Used during Phases 0â€“4 (demo mode). Spring's primary bean â€” will be
 * superseded by ShopifyStoreDataProvider / GA4StoreDataProvider in Phase 5
 * via @Qualifier or strategy pattern.
 */
@Component
@RequiredArgsConstructor
public class DummyStoreDataProvider implements StoreDataProvider {

    private final DummyStoreRepository storeRepository;
    private final DummyProductRepository productRepository;
    private final DummySessionEventRepository sessionEventRepository;
    private final DummyDailyOrderRepository orderRepository;
    private final DummyProductKeywordRepository keywordRepository;

    @Override
    public DummyStore getStore(String storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new NoSuchElementException("Store not found: " + storeId));
    }

    @Override
    public List<DummyProduct> getProducts(String storeId) {
        return productRepository.findByStoreId(storeId);
    }

    @Override
    public List<DummySessionEvent> getSessionEvents(String storeId, LocalDate from, LocalDate to) {
        return sessionEventRepository.findByStoreIdAndEventDateBetween(storeId, from, to);
    }

    @Override
    public List<DummyDailyOrder> getOrders(String storeId, LocalDate from, LocalDate to) {
        return orderRepository.findByStoreIdAndOrderDateBetween(storeId, from, to);
    }

    @Override
    public List<DummyProductKeyword> getProductKeywords(String storeId) {
        List<String> productIds = productRepository.findByStoreId(storeId)
                .stream()
                .map(DummyProduct::getProductId)
                .collect(Collectors.toList());
        return keywordRepository.findByProductIdIn(productIds);
    }

    @Override
    public String getDataSource() {
        return "dummy";
    }
}

