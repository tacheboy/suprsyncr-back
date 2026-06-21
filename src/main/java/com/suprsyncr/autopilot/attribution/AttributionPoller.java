package com.suprsyncr.autopilot.attribution;

import com.suprsyncr.order.entity.Order;
import com.suprsyncr.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scheduled scan for recent orders that have not yet been attribution-checked.
 *
 * Runs every {@code attribution.poll-interval-ms} milliseconds and:
 *   1. Fetches recent orders newer than {@code attribution.poll-lookback-min}.
 *   2. Removes orders that already have an Attribution row.
 *   3. Calls {@link AttributionService#analyzeOrder(Long)} for each remaining.
 *
 * The poller is the "agent wakes up by itself" half of Scenario 3. The
 * {@link AttributionController}'s POST /analyze/{orderId} provides the
 * manual trigger half (the demo "analyze this sale" button).
 *
 * Idempotency comes from the Attribution row's UNIQUE(order_id) constraint —
 * if a row exists for an order the service short-circuits, so concurrent
 * triggers are safe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AttributionPoller {

    private final OrderRepository orderRepository;
    private final AttributionRepository attributionRepository;
    private final AttributionService attributionService;

    @Value("${attribution.poll-enabled:true}")
    private boolean enabled;

    @Value("${attribution.poll-lookback-min:30}")
    private int lookbackMinutes;

    @Value("${attribution.poll-batch:50}")
    private int batchSize;

    /**
     * Fixed delay (not rate) so a slow batch doesn't pile new scans on top.
     * Initial delay of 20s gives the engine + DB time to settle on a fresh boot.
     */
    @Scheduled(fixedDelayString = "${attribution.poll-interval-ms:60000}",
               initialDelay = 20_000)
    public void scan() {
        if (!enabled) return;

        LocalDateTime since = LocalDateTime.now().minusMinutes(lookbackMinutes);
        List<Order> recent = orderRepository.findRecentOrders(since,
                PageRequest.of(0, batchSize));
        if (recent.isEmpty()) return;

        List<Long> orderIds = recent.stream().map(Order::getId).toList();
        Set<Long> already = new HashSet<>(
                attributionRepository.findExistingOrderIds(orderIds));

        int processed = 0;
        for (Order order : recent) {
            if (already.contains(order.getId())) continue;
            try {
                Attribution row = attributionService.analyzeOrder(order.getId());
                log.info("attribution poller: order {} → {}",
                        order.getId(), row.getStatus());
                processed++;
            } catch (Exception e) {
                log.warn("attribution poller: order {} errored: {}",
                        order.getId(), e.getMessage());
            }
        }
        if (processed > 0) {
            log.info("attribution poller: scanned {} recent orders, processed {}",
                    recent.size(), processed);
        }
    }
}
