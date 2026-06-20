package com.suprsyncr.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Represents a dummy D2C store used for analytics demo mode.
 */
@Entity
@Table(name = "dummy_stores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DummyStore {

    @Id
    @Column(name = "store_id")
    private String storeId;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "monthly_traffic", nullable = false)
    private Integer monthlyTraffic;

    @Column(name = "avg_order_value", nullable = false)
    private BigDecimal avgOrderValue;

    @Column(name = "primary_traffic_source", nullable = false)
    private String primaryTrafficSource;
}

