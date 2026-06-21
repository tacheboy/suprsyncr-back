package com.suprsyncr.integration.shopify;

import com.suprsyncr.auth.entity.User;
import com.suprsyncr.auth.repository.UserRepository;
import com.suprsyncr.auth.service.AuthService;
import com.suprsyncr.common.dto.ApiResponse;
import com.suprsyncr.common.security.JwtService;
import com.suprsyncr.seller.entity.Seller;
import com.suprsyncr.seller.repository.SellerRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ShopifyOAuthController {

    private static final Logger log = LoggerFactory.getLogger(ShopifyOAuthController.class);

    private final ShopifyOAuthService shopifyOAuthService;
    private final ShopifyOAuthConfig oauthConfig;
    private final AuthService authService;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public ShopifyOAuthController(
            ShopifyOAuthService shopifyOAuthService,
            ShopifyOAuthConfig oauthConfig,
            AuthService authService,
            SellerRepository sellerRepository,
            UserRepository userRepository,
            JwtService jwtService) {
        this.shopifyOAuthService = shopifyOAuthService;
        this.oauthConfig = oauthConfig;
        this.authService = authService;
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * OAuth Authorization Endpoint.
     * Opened directly by a popup tab, so it cannot carry an Authorization header —
     * the frontend appends {@code ?token=<jwt>} to authenticate, and this method
     * validates that token explicitly (it bypasses the standard JWT filter).
     */
    @GetMapping("/api/v1/shopify/auth")
    public void initiateOAuth(
            @RequestParam("shop") String shop,
            @RequestParam(value = "token", required = false) String token,
            HttpServletResponse response) throws IOException {

        // Fail fast & loud when the operator hasn't configured a Shopify Partner app.
        // Without these the redirect to Shopify would be a broken URL or generic 500;
        // a clear 503 saves people from chasing the wrong bug.
        if (oauthConfig.getClientId() == null || oauthConfig.getClientId().isBlank()
                || oauthConfig.getClientSecret() == null || oauthConfig.getClientSecret().isBlank()) {
            log.warn("Shopify OAuth attempted but SHOPIFY_CLIENT_ID/SHOPIFY_CLIENT_SECRET are unset");
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Shopify integration is not configured on this deployment. " +
                    "Set SHOPIFY_CLIENT_ID and SHOPIFY_CLIENT_SECRET (from your Shopify Partner app) " +
                    "and ensure SHOPIFY_REDIRECT_URI is a publicly reachable URL " +
                    "(use ngrok / cloudflared while developing locally).");
            return;
        }

        // Authenticate via the token query parameter (no Authorization header here).
        User currentUser = resolveUserFromToken(token);
        if (currentUser == null) {
            log.warn("Shopify OAuth: missing or invalid token query parameter");
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Not signed in. Please log in again and retry the connection.");
            return;
        }

        // Validate shop domain
        if (!shopifyOAuthService.isValidShopDomain(shop)) {
            log.warn("Invalid shop domain received: {}", shop);
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid shop domain. Expected format: store-name.myshopify.com");
            return;
        }

        Seller seller = sellerRepository.findByUserId(currentUser.getId()).orElse(null);
        if (seller == null) {
            log.warn("No seller profile found for user: {}", currentUser.getId());
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Seller profile not found. Complete onboarding first.");
            return;
        }

        // Build authorization URL with CSRF state and 302 to Shopify consent screen.
        String authorizationUrl = shopifyOAuthService.buildAuthorizationUrl(shop, seller.getId());
        response.sendRedirect(authorizationUrl);
    }

    /**
     * Write the error directly to the response. {@code response.sendError} would
     * trigger an internal forward to Spring's /error page, which (correctly) requires
     * authentication on this stack and turns every controller-side error into a 403.
     * Writing the body directly keeps the user-facing status accurate.
     */
    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/html;charset=UTF-8");
        String safe = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        response.getWriter().write(
                "<!doctype html><meta charset='utf-8'><title>Shopify Connection Error</title>" +
                "<style>body{font:14px/1.5 system-ui;padding:32px;color:#1f2937;background:#fafafa;max-width:640px;margin:0 auto}" +
                "h1{font-size:18px;margin:0 0 12px}code{background:#f3f4f6;padding:2px 6px;border-radius:4px}</style>" +
                "<h1>Cannot start Shopify connection</h1><p>" + safe + "</p>" +
                "<script>setTimeout(function(){try{window.close()}catch(e){}},6000);" +
                "if(window.opener){window.opener.postMessage({type:'shopify-connected',success:false,error:" +
                escapeJs(message) + "},'*')}</script>"
        );
    }

    /**
     * Re-registers all Shopify order webhook topics for the authenticated seller's store.
     * Safe to call multiple times (idempotent). Requires SHOPIFY_WEBHOOK_BASE_URL to be
     * configured to a public HTTPS URL — returns a hint in the response when it isn't.
     */
    @PostMapping("/api/v1/shopify/webhooks/register")
    public ResponseEntity<?> registerWebhooks() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Seller seller = sellerRepository.findByUserId(currentUser.getId()).orElse(null);
        if (seller == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, null, "Seller profile not found", LocalDateTime.now()));
        }
        try {
            return ResponseEntity.ok(ApiResponse.success(shopifyOAuthService.reRegisterWebhooks(seller.getId())));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, null, e.getMessage(), LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Webhook re-registration failed for seller {}: {}", seller.getId(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, null, "Webhook registration failed: " + e.getMessage(), LocalDateTime.now()));
        }
    }

    private String escapeJs(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'";
    }

    /** Decode and verify the JWT passed via query string, return the user or null. */
    private User resolveUserFromToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            String email = jwtService.extractUsername(token);
            if (email == null || email.isBlank()) return null;
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to decode JWT from query string: {}", e.getMessage());
            return null;
        }
    }

    /**
     * OAuth Callback Endpoint.
     * Shopify redirects here after the merchant authorizes.
     * Validates HMAC + state, exchanges code for token, saves connection,
     * and returns HTML that communicates back to the parent window.
     *
     * Note: we accept the full parameter map (not a fixed @RequestParam list)
     * because Shopify computes the HMAC over EVERY query param it sends except
     * {@code hmac} itself — and that set isn't fixed (e.g. {@code host},
     * {@code embedded}). Pulling only a known subset would always fail HMAC.
     */
    @GetMapping("/shopify/callback")
    public ResponseEntity<String> handleCallback(@RequestParam Map<String, String> allParams) {

        String code = allParams.get("code");
        String shop = allParams.get("shop");
        String state = allParams.get("state");
        String hmac = allParams.get("hmac");

        if (code == null || shop == null || state == null || hmac == null) {
            log.error("Shopify callback missing required params; have keys={}", allParams.keySet());
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorHtml("Missing required callback parameters."));
        }

        log.info("Shopify OAuth callback received for shop: {} (params: {})", shop, allParams.keySet());

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

        // 3. Validate HMAC over the FULL query string Shopify sent (minus hmac itself).
        if (!shopifyOAuthService.validateHmac(allParams)) {
            log.error("HMAC validation failed for shop: {} (params used: {})", shop, allParams.keySet());
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
