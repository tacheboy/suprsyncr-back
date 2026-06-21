package com.suprsyncr.order.controller;

import com.suprsyncr.notification.entity.NotificationType;
import com.suprsyncr.notification.service.StoreNotificationService;
import com.suprsyncr.order.entity.EventTrigger;
import com.suprsyncr.order.entity.Order;
import com.suprsyncr.order.entity.OrderEvent;
import com.suprsyncr.order.entity.OrderItem;
import com.suprsyncr.order.entity.OrderStatus;
import com.suprsyncr.order.repository.OrderEventRepository;
import com.suprsyncr.order.repository.OrderRepository;
import com.suprsyncr.product.entity.Product;
import com.suprsyncr.product.entity.ProductVariant;
import com.suprsyncr.product.repository.ProductRepository;
import com.suprsyncr.product.repository.ProductVariantRepository;
import com.suprsyncr.seller.entity.SellerPlatform;
import com.suprsyncr.seller.repository.SellerPlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Demo-only endpoint: creates a real Order row with a product-linked OrderItem
 * so the Scenario 3 attribution poller has something deterministic to scan.
 *
 * NOT for production use. Expose only behind an internal or demo profile
 * when deploying outside of a demo environment.
 */
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@Slf4j
public class DemoController {

    private final SellerPlatformRepository platformRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final StoreNotificationService notificationService;

    /**
     * Creates a seed order for the given platform and product. The order item
     * is linked to the product's first variant so attribution can traverse
     * item → productVariant → product → sku.
     *
     * @param platformId the SellerPlatform id (the store to attach the order to)
     * @param productId  the Product id whose first variant will be ordered
     */
    @PostMapping("/seed-order")
    @Transactional
    public ResponseEntity<Map<String, Object>> seedOrder(
            @RequestParam Long platformId,
            @RequestParam Long productId) {

        SellerPlatform platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new IllegalArgumentException("Platform not found: " + platformId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<ProductVariant> variants = variantRepository.findByProductId(productId);
        ProductVariant variant = variants.isEmpty() ? null : variants.get(0);

        BigDecimal price = product.getBasePrice() != null
                ? product.getBasePrice() : BigDecimal.valueOf(999);

        Order order = new Order();
        order.setSeller(platform.getSeller());
        order.setPlatform(platform);
        order.setExternalOrderId("demo-" + System.currentTimeMillis());
        order.setUspOrderId(generateDemoOrderId());
        order.setStatus(OrderStatus.PENDING);
        order.setCustomerName("Demo Customer");
        order.setShippingAddress("123 Demo Street, Demo City");
        order.setCustomerPhone("+91-9999999999");
        order.setCustomerEmail("demo@suprsyncr.com");
        order.setOrderedAt(LocalDateTime.now());
        order.setTotalAmount(price);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductVariant(variant);
        item.setProductName(product.getName());
        item.setVariantName(variant != null ? variant.getVariantName() : "Default");
        item.setQuantity(1);
        item.setUnitPrice(price);
        item.setTotalPrice(price);

        List<OrderItem> items = new ArrayList<>();
        items.add(item);
        order.setItems(items);

        Order saved = orderRepository.save(order);

        OrderEvent event = new OrderEvent();
        event.setOrder(saved);
        event.setFromStatus(null);
        event.setToStatus(OrderStatus.PENDING);
        event.setNotes("Demo seed order created");
        event.setTriggeredBy(EventTrigger.PLATFORM);
        orderEventRepository.save(event);

        log.info("demo: seeded order {} for product {} (variant={})",
                saved.getId(), productId, variant != null ? variant.getId() : "none");

        try {
            notificationService.createInternalNotification(
                    platform, NotificationType.NEW_ORDER, "orders/demo",
                    saved.getExternalOrderId(), "Demo Customer", price, "INR");
        } catch (Exception e) {
            log.warn("demo notification skipped: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "orderId", saved.getId(),
                "externalOrderId", saved.getExternalOrderId(),
                "uspOrderId", saved.getUspOrderId(),
                "productId", productId,
                "variantLinked", variant != null,
                "totalAmount", price,
                "message", "Seed order created. Attribution poller will pick it up within 60s."
        ));
    }

    private static String generateDemoOrderId() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%06d", new Random().nextInt(1_000_000));
        return "DEMO-" + datePart + "-" + randomPart;
    }
}
