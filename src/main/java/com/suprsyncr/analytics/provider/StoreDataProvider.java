package com.suprsyncr.analytics.provider;

import com.suprsyncr.analytics.domain.DummyDailyOrder;
import com.suprsyncr.analytics.domain.DummyProduct;
import com.suprsyncr.analytics.domain.DummyProductKeyword;
import com.suprsyncr.analytics.domain.DummySessionEvent;
import com.suprsyncr.analytics.domain.DummyStore;

import java.time.LocalDate;
import java.util.List;

/**
 * Abstraction layer for all store data access.
 *
 * The analytics services (RevenueLeakService, ProductHealthService, SeoGapService)
 * ALWAYS go through this interface. They never know whether they are talking to
 * dummy data, Shopify, or GA4.
 *
 * Implementations:
 *  - DummyStoreDataProvider  (Phase 0â€“4, reads from dummy_* tables)
 *  - ShopifyStoreDataProvider (Phase 5, reads from Shopify API)
 *  - GA4StoreDataProvider     (Phase 5, reads from GA4 API)
 */
public interface StoreDataProvider {

    /** Resolve the store metadata (name, AOV, primary traffic source, etc.) */
    DummyStore getStore(String storeId);

    /** All products with funnel metrics */
    List<DummyProduct> getProducts(String storeId);

    /** Session funnel events for a date range */
    List<DummySessionEvent> getSessionEvents(String storeId, LocalDate from, LocalDate to);

    /** Order history for a date range */
    List<DummyDailyOrder> getOrders(String storeId, LocalDate from, LocalDate to);

    /** Keyword-level SEO data for all products in a store */
    List<DummyProductKeyword> getProductKeywords(String storeId);

    /**
     * Returns "dummy" or "live". Used in API responses so the frontend
     * can show/hide the Demo Mode banner.
     */
    String getDataSource();
}

