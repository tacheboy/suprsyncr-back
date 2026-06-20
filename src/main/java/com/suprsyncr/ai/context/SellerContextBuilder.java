package com.suprsyncr.ai.context;

import com.suprsyncr.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SellerContextBuilder {
    private final OrderRepository orderRepository;

    public String buildRichContext(Long sellerId, int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        LocalDateTime end = LocalDateTime.now();

        Map<String, Object> stats = orderRepository.calculateOrderStatistics(sellerId, start, end);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Performance over the last %d days:\n", days));
        
        Number totalOrdersNum = stats != null ? (Number) stats.get("totalOrders") : null;
        long totalOrders = totalOrdersNum != null ? totalOrdersNum.longValue() : 0;

        if (stats != null && !stats.isEmpty() && totalOrders > 0) {
            sb.append("- Total Orders: ").append(totalOrders).append("\n");
            
            Object revenue = stats.get("totalRevenue");
            sb.append("- Total Revenue (Delivered): INR ").append(revenue != null ? revenue : 0).append("\n");
            
            sb.append("- Pending Processing: ").append(getAsInt(stats, "pendingOrders")).append("\n");
            
            int shippedDelivered = getAsInt(stats, "shippedOrders") + getAsInt(stats, "deliveredOrders");
            sb.append("- Shipped/Delivered: ").append(shippedDelivered).append("\n");
            
            int cancelledReturned = getAsInt(stats, "cancelledOrders") + getAsInt(stats, "returnedOrders");
            sb.append("- Cancelled/Returned: ").append(cancelledReturned).append("\n");
        } else {
            sb.append("No order data available for this period.\n");
        }

        sb.append("\nNote: Complete historical analytics integration in progress.\n");

        return sb.toString();
    }

    private int getAsInt(Map<String, Object> stats, String key) {
        if (stats == null) return 0;
        Number num = (Number) stats.get(key);
        return num != null ? num.intValue() : 0;
    }
}

