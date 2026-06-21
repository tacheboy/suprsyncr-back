package com.suprsyncr.integration.shopify;

import com.suprsyncr.auth.service.AuthService;
import com.suprsyncr.common.dto.ApiResponse;
import com.suprsyncr.integration.shopify.dto.CreateTestOrderRequest;
import com.suprsyncr.seller.entity.Seller;
import com.suprsyncr.seller.entity.SellerPlatform;
import com.suprsyncr.seller.repository.SellerPlatformRepository;
import com.suprsyncr.seller.repository.SellerRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Authenticated developer/sandbox utility; it never accepts Shopify credentials from the client. */
@RestController
@RequestMapping("/api/v1/shopify/platforms")
public class ShopifyTestOrderController {

    private final AuthService authService;
    private final SellerRepository sellerRepository;
    private final SellerPlatformRepository platformRepository;
    private final ShopifyTestOrderService testOrderService;
    private final ShopifyWebhookRegistrationService webhookRegistrationService;

    public ShopifyTestOrderController(AuthService authService, SellerRepository sellerRepository,
                                      SellerPlatformRepository platformRepository,
                                      ShopifyTestOrderService testOrderService,
                                      ShopifyWebhookRegistrationService webhookRegistrationService) {
        this.authService = authService;
        this.sellerRepository = sellerRepository;
        this.platformRepository = platformRepository;
        this.testOrderService = testOrderService;
        this.webhookRegistrationService = webhookRegistrationService;
    }

    @PostMapping("/{platformId}/test-orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTestOrder(
            @PathVariable Long platformId, @Valid @RequestBody CreateTestOrderRequest request) {
        SellerPlatform platform = ownedPlatform(platformId);
        return ResponseEntity.ok(ApiResponse.success(testOrderService.createTestOrder(platform, request)));
    }

    @PostMapping("/{platformId}/webhooks/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerWebhooks(@PathVariable Long platformId) {
        boolean registered = webhookRegistrationService.registerOrderWebhooks(ownedPlatform(platformId));
        return ResponseEntity.ok(ApiResponse.success(Map.of("registered", registered)));
    }

    private SellerPlatform ownedPlatform(Long platformId) {
        Seller seller = sellerRepository.findByUserId(authService.getCurrentUser().getId())
                .orElseThrow(() -> new IllegalStateException("Seller profile not found"));
        SellerPlatform platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new IllegalArgumentException("Platform not found"));
        if (!platform.getSeller().getId().equals(seller.getId())) {
            throw new IllegalArgumentException("Platform does not belong to the current seller");
        }
        return platform;
    }
}
