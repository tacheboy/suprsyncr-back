package com.suprsyncr.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Keyword-level SEO data for a dummy product (Search Console proxy).
 */
@Entity
@Table(name = "dummy_product_keywords")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DummyProductKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "query", nullable = false)
    private String query;

    @Column(name = "impressions")
    private Integer impressions;

    @Column(name = "clicks")
    private Integer clicks;

    @Column(name = "position")
    private BigDecimal position;

    @Column(name = "ctr")
    private BigDecimal ctr;
}

