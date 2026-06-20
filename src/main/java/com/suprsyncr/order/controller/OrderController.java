package com.suprsyncr.order.controller;

import com.suprsyncr.common.dto.ApiResponse;
import com.suprsyncr.common.dto.PageResponse;
import com.suprsyncr.order.dto.*;
import com.suprsyncr.order.entity.OrderStatus;
import com.suprsyncr.order.service.OrderService;
import com.suprsyncr.seller.entity.PlatformType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for order management operations.
 * Handles order lifecycle, fulfillment, and statistics.
 * 
 * Requirements: 16, 17, 18, 19, 20, 21, 96
 */
@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Orders", description = "Order management and fulfillment endpoints")
public class OrderController {
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    /**
     * Get a single order by ID.
     * 
     * @param orderId the order ID
     * @return ApiResponse containing order details
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "Get order", description = "Retrieves a single order by ID with all items and events")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(@PathVariable Long orderId) {
        OrderDto order = orderService.getOrder(orderId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                order,
                "Order retrieved successfully",
                LocalDateTime.now()
        ));
    }
    
    /**
     * Get orders with pagination and filters.
     * 
     * @param page the page number (default 0)
     * @param size the page size (default 20, max 100)
     * @param status optional order status filter
     * @param platformType optional platform type filter
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @return PageResponse containing filtered orders
     */
    @GetMapping
    @Operation(summary = "Get orders", description = "Retrieves paginated orders with optional filters for status, platform, and date range")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<PageResponse<OrderDto>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PlatformType platformType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        Page<OrderDto> orders = orderService.getOrders(page, size, status, platformType, startDate, endDate);
        PageResponse<OrderDto> pageResponse = new PageResponse<>(
                orders.getContent(),
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages()
        );
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                pageResponse,
                "Orders retrieved successfully",
                LocalDateTime.now()
        ));
    }
    
    /**
     * Get order statistics with optional date range filters.
     * 
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @return ApiResponse containing order statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get order statistics", description = "Retrieves order statistics including counts by status and total revenue")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order statistics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<OrderStatsDto>> getOrderStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        OrderStatsDto stats = orderService.getOrderStats(startDate, endDate);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                stats,
                "Order statistics retrieved successfully",
                LocalDateTime.now()
        ));
    }
    
    /**
     * Accept an order and reserve inventory.
     * 
     * @param orderId the order ID
     * @param request the accept order request with warehouse ID
     * @return ApiResponse containing updated order
     */
    @PostMapping("/{orderId}/accept")
    @Operation(summary = "Accept order", description = "Accepts an order and reserves inventory from the specified warehouse")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order accepted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid state transition"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Insufficient stock"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<OrderDto>> acceptOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody AcceptOrderRequest request
    ) {
        OrderDto order = orderService.acceptOrder(orderId, request);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                order,
                "Order accepted successfully",
                LocalDateTime.now()
        ));
    }
    
    /**
     * Ship an order and commit inventory.
     * 
     * @param orderId the order ID
     * @param request the ship order request with tracking details
     * @return ApiResponse containing updated order
     */
    @PostMapping("/{orderId}/ship")
    @Operation(summary = "Ship order", description = "Marks an order as shipped, commits inventory, and updates tracking information")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order shipped successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid state transition"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<OrderDto>> shipOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody ShipOrderRequest request
    ) {
        OrderDto order = orderService.shipOrder(orderId, request);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                order,
                "Order shipped successfully",
                LocalDateTime.now()
        ));
    }
    
    /**
     * Mark an order as delivered.
     * 
     * @param orderId the order ID
     * @return ApiResponse containing updated order
     */
    @PostMapping("/{orderId}/deliver")
    @Operation(summary = "Mark order delivered", description = "Marks an order as delivered")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order marked as delivered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid state transition"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<OrderDto>> markDelivered(@PathVariable Long orderId) {
        OrderDto order = orderService.markDelivered(orderId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                order,
                "Order marked as delivered successfully",
                LocalDateTime.now()
        ));
    }
    
    /**
     * Cancel an order and release reserved inventory if applicable.
     * 
     * @param orderId the order ID
     * @param request the cancel order request with reason
     * @return ApiResponse containing updated order
     */
    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancels an order and releases reserved inventory if applicable")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid state transition"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        OrderDto order = orderService.cancelOrder(orderId, request);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                order,
                "Order cancelled successfully",
                LocalDateTime.now()
        ));
    }
    
    /**
     * Get event history for an order.
     * 
     * @param orderId the order ID
     * @return ApiResponse containing list of order events
     */
    @GetMapping("/{orderId}/events")
    @Operation(summary = "Get order events", description = "Retrieves the event history for an order showing all status transitions")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order events retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<OrderEventDto>>> getOrderEvents(@PathVariable Long orderId) {
        List<OrderEventDto> events = orderService.getOrderEvents(orderId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                events,
                "Order events retrieved successfully",
                LocalDateTime.now()
        ));
    }
}

