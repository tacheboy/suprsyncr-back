package com.suprsyncr.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Represents a daily session funnel event for a store, per traffic source.
 */
@Entity
@Table(name = "dummy_session_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DummySessionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "sessions")
    private Integer sessions;

    @Column(name = "add_to_cart_sessions")
    private Integer addToCartSessions;

    @Column(name = "checkout_init_sessions")
    private Integer checkoutInitSessions;

    @Column(name = "purchased_sessions")
    private Integer purchasedSessions;
}

