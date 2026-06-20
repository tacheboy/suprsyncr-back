package com.suprsyncr.integration.shopify;

import com.suprsyncr.auth.entity.User;
import com.suprsyncr.auth.service.AuthService;
import com.suprsyncr.common.dto.ApiResponse;
import com.suprsyncr.seller.entity.Seller;
import com.suprsyncr.seller.repository.SellerRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ShopifyOAuthController {

    private static final Logger log = LoggerFactory.getLogger(ShopifyOAuthController.class);

    private final ShopifyOAuthService shopifyOAuthService;
    private final AuthService authService;
    private final SellerRepository sellerRepository;

    public ShopifyOAuthController(
            ShopifyOAuthService shopifyOAuthService,
            AuthService authService,
            SellerRepository sellerRepository) {
        this.shopifyOAuthService = shopifyOAuthService;
        this.authService = authService;
        this.sellerRepository = sellerRepository;
    }

    /**
     * OAuth Authorization Endpoint.
     * Called from the frontend popup window to initiate Shopify OAuth.
     * Validates the shop parameter, generates state, and redirects to Shopify.
     */
    @GetMapping("/api/v1/shopify/auth")
    public void initiateOAuth(
            @RequestParam("shop") String shop,
            HttpServletResponse response) throws IOException {

        // Validate shop domain
        if (!shopifyOAuthService.isValidShopDomain(shop)) {
            log.warn("Invalid shop domain received: {}", shop);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid shop domain. Expected format: store-name.myshopify.com");
            return;
        }

        // Get current authenticated seller
        User currentUser = authService.getCurrentUser();
        Seller seller = sellerRepository.findByUserId(currentUser.getId())
                .orElse(null);

        if (seller == null) {
            log.warn("No seller profile found for user: {}", currentUser.getId());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Seller profile not found. Complete onboarding first.");
            return;
        }

        // Build authorization URL with CSRF state
        String authorizationUrl = shopifyOAuthService.buildAuthorizationUrl(shop, seller.getId());

        // 302 redirect to Shopify OAuth consent screen
        response.sendRedirect(authorizationUrl);
    }

    /**
     * OAuth Callback Endpoint.
     * Shopify redirects here after the merchant authorizes.
     * Validates HMAC + state, exchanges code for token, saves connection,
     * and returns HTML that communicates back to the parent window.
     */
    @GetMapping("/shopify/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam("code") String code,
            @RequestParam("shop") String shop,
            @RequestParam("state") String state,
            @RequestParam("hmac") String hmac,
            @RequestParam(value = "timestamp", required = false) String timestamp) {

        log.info("Shopify OAuth callback received for shop: {}", shop);

        // 1. Validate state (CSRF protection)
        ShopifyOAuthService.OAuthStateEntry stateEntry = shopifyOAuthService.validateAndConsumeState(state);
        if (stateEntry == null) {
            log.error("Invalid or expired OAuth state for shop: {}", shop);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorHtml("Invalid or expired authorization state. Please try again."));
        }

        // 2. Validate shop matches the original request
        if (!stateEntry.shop().equals(shop)) {
            log.error("Shop mismatch in callback. Expected: {}, Got: {}", stateEntry.shop(), shop);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorHtml("Shop domain mismatch. Please try again."));
        }

        // 3. Validate HMAC signature
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        params.put("shop", shop);
        params.put("state", state);
        params.put("hmac", hmac);
        if (timestamp != null) {
            params.put("timestamp", timestamp);
        }

        if (!shopifyOAuthService.validateHmac(params)) {
            log.error("HMAC validation failed for shop: {}", shop);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorHtml("Signature validation failed. Please try again."));
        }

        // 4. Exchange code for access token
        String accessToken;
        try {
            accessToken = shopifyOAuthService.exchangeCodeForToken(shop, code);
        } catch (IOException e) {
            log.error("Failed to exchange code for token. Shop: {}", shop, e);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorHtml("Failed to connect to Shopify. Please try again."));
        }

        // 5. Save the connection to the database
        try {
            shopifyOAuthService.saveShopifyConnection(stateEntry.sellerId(), shop, accessToken);
        } catch (Exception e) {
            log.error("Failed to save Shopify connection. Shop: {}, Seller: {}", shop, stateEntry.sellerId(), e);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorHtml("Failed to save connection. Please try again."));
        }

        // 6. Return success HTML that communicates with the parent window
        log.info("Shopify OAuth completed successfully for shop: {}, seller: {}", shop, stateEntry.sellerId());
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(buildSuccessHtml());
    }

    private String buildSuccessHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <title>Connecting Shopify...</title>
                </head>
                <body>
                  <script>
                    if (window.opener) {
                      window.opener.postMessage({ type: 'shopify-connected', success: true }, '*');
                    }
                    window.close();
                  </script>
                  <p>Successfully connected! Closing window...</p>
                </body>
                </html>
                """;
    }

    private String buildErrorHtml(String errorMessage) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <title>Shopify Connection Error</title>
                </head>
                <body>
                  <script>
                    if (window.opener) {
                      window.opener.postMessage({ type: 'shopify-connected', success: false, error: '%s' }, '*');
                    }
                    setTimeout(function() { window.close(); }, 3000);
                  </script>
                  <p>Error: %s</p>
                  <p>This window will close automatically...</p>
                </body>
                </html>
                """.formatted(errorMessage.replace("'", "\\'"), errorMessage);
    }
}
