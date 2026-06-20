package com.suprsyncr.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single daily order in a dummy D2C store.
 */
@Entity
@Table(name = "dummy_daily_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DummyDailyOrder {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "revenue", nullable = false)
    private BigDecimal revenue;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "channel")
    private String channel;

    @Column(name = "customer_city")
    private String customerCity;

    @Column(name = "customer_gender")
    private String customerGender;

    @Column(name = "age_group")
    private String ageGroup;
}

