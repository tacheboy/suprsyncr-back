package com.suprsyncr.notification.controller;

import com.suprsyncr.auth.service.AuthService;
import com.suprsyncr.common.dto.ApiResponse;
import com.suprsyncr.notification.dto.StoreNotificationDto;
import com.suprsyncr.notification.service.StoreNotificationService;
import com.suprsyncr.seller.entity.Seller;
import com.suprsyncr.seller.repository.SellerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class StoreNotificationController {

    private final AuthService authService;
    private final SellerRepository sellerRepository;
    private final StoreNotificationService notificationService;

    public StoreNotificationController(AuthService authService, SellerRepository sellerRepository,
                                       StoreNotificationService notificationService) {
        this.authService = authService;
        this.sellerRepository = sellerRepository;
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreNotificationDto>>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "50") int limit) {
        Seller seller = currentSeller();
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(seller.getId(), unreadOnly, limit)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        Seller seller = currentSeller();
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", notificationService.unreadCount(seller.getId()))));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<StoreNotificationDto>> markRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markRead(currentSeller().getId(), notificationId)));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllRead(currentSeller().getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Seller currentSeller() {
        return sellerRepository.findByUserId(authService.getCurrentUser().getId())
                .orElseThrow(() -> new IllegalStateException("Seller profile not found"));
    }
}
