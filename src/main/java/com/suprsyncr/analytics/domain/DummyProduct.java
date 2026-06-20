package com.suprsyncr.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Represents a product in a dummy D2C store with full funnel metrics.
 */
@Entity
@Table(name = "dummy_products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DummyProduct {

    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category")
    private String category;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "stock")
    private Integer stock;

    // Funnel counts (monthly)
    @Column(name = "monthly_page_views")
    private Integer monthlyPageViews;

    @Column(name = "add_to_cart_count")
    private Integer addToCartCount;

    @Column(name = "checkout_count")
    private Integer checkoutCount;

    @Column(name = "purchase_count")
    private Integer purchaseCount;

    // Search Console proxy
    @Column(name = "organic_impressions")
    private Integer organicImpressions;

    @Column(name = "organic_clicks")
    private Integer organicClicks;

    @Column(name = "avg_position")
    private BigDecimal avgPosition;

    @Column(name = "primary_traffic_source")
    private String primaryTrafficSource;
}

