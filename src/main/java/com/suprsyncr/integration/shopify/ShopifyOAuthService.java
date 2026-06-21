package com.suprsyncr.integration.shopify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.auth.entity.User;
import com.suprsyncr.auth.service.AuthService;
import com.suprsyncr.autopilot.domain.AgentRun;
import com.suprsyncr.autopilot.service.AgentRunOrchestratorService;
import com.suprsyncr.seller.entity.*;
import com.suprsyncr.seller.repository.SellerPlatformRepository;
import com.suprsyncr.seller.repository.SellerRepository;
import com.suprsyncr.seller.service.CredentialEncryptionService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ShopifyOAuthService {

    private static final Logger log = LoggerFactory.getLogger(ShopifyOAuthService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final ShopifyOAuthConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SellerRepository sellerRepository;
    private final SellerPlatformRepository platformRepository;
    private final CredentialEncryptionService encryptionService;
    private final AuthService authService;
    private final AgentRunOrchestratorService autopilotOrchestrator;
    private final ShopifyCatalogueSyncService catalogueSyncService;
    private final ShopifyWebhookRegistrationService webhookRegistrationService;

    // In-memory state store for OAuth CSRF protection.
    // Key: state nonce, Value: OAuthStateEntry (shop + sellerId + timestamp)
    private final ConcurrentHashMap<String, OAuthStateEntry> stateStore = new ConcurrentHashMap<>();

    public ShopifyOAuthService(
            ShopifyOAuthConfig config,
            OkHttpClient httpClient,
            ObjectMapper objectMapper,
            SellerRepository sellerRepository,
            SellerPlatformRepository platformRepository,
            CredentialEncryptionService encryptionService,
            AuthService authService,
            @Lazy AgentRunOrchestratorService autopilotOrchestrator,
            ShopifyCatalogueSyncService catalogueSyncService,
            ShopifyWebhookRegistrationService webhookRegistrationService) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.sellerRepository = sellerRepository;
        this.platformRepository = platformRepository;
        this.encryptionService = encryptionService;
        this.autopilotOrchestrator = autopilotOrchestrator;
        this.catalogueSyncService = catalogueSyncService;
        this.webhookRegistrationService = webhookRegistrationService;
        this.authService = authService;
    }

    /**
     * Validates the shop parameter format.
     */
    public boolean isValidShopDomain(String shop) {
        if (shop == null || shop.isBlank()) {
            return false;
        }
        // Shopify shop domains must match: {store-name}.myshopify.com
        return shop.matches("^[a-zA-Z0-9][a-zA-Z0-9\\-]*\\.myshopify\\.com$");
    }

    /**
     * Generates the Shopify OAuth authorization URL and stores the state for CSRF validation.
     */
    public String buildAuthorizationUrl(String shop, Long sellerId) {
        String state = generateSecureState();

        // Store state with metadata for validation on callback
        stateStore.put(state, new OAuthStateEntry(shop, sellerId, System.currentTimeMillis()));

        // Clean up expired state entries (older than 10 minutes)
        cleanExpiredStates();

        String authUrl = String.format(
                "https://%s/admin/oauth/authorize?client_id=%s&scope=%s&redirect_uri=%s&state=%s",
                shop,
                URLEncoder.encode(config.getClientId(), StandardCharsets.UTF_8),
                URLEncoder.encode(config.getScopes(), StandardCharsets.UTF_8),
                URLEncoder.encode(config.getRedirectUri(), StandardCharsets.UTF_8),
                URLEncoder.encode(state, StandardCharsets.UTF_8)
        );

        log.info("Generated Shopify OAuth URL for shop: {}", shop);
        return authUrl;
    }

    /**
     * Validates the HMAC signature from Shopify callback parameters.
     */
    public boolean validateHmac(Map<String, String> params) {
        String hmac = params.get("hmac");
        if (hmac == null) {
            return false;
        }

        // Build the message by sorting params (excluding hmac) and joining with &
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("hmac");

        StringBuilder message = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (message.length() > 0) {
                message.append("&");
            }
            message.append(entry.getKey()).append("=").append(entry.getValue());
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    config.getClientSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(message.toString().getBytes(StandardCharsets.UTF_8));

            String computedHmac = bytesToHex(rawHmac);
            return constantTimeEquals(computedHmac, hmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC validation failed due to crypto error", e);
            return false;
        }
    }

    /**
     * Validates the state parameter and returns the associated entry, or null if invalid.
     */
    public OAuthStateEntry validateAndConsumeState(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        OAuthStateEntry entry = stateStore.remove(state);
        if (entry == null) {
            log.warn("OAuth state not found or already consumed: {}", state);
            return null;
        }
        // Check expiry (10 minutes)
        if (System.currentTimeMillis() - entry.createdAt() > 600_000) {
            log.warn("OAuth state expired for shop: {}", entry.shop());
            return null;
        }
        return entry;
    }

    /**
     * Exchanges the authorization code for a permanent access token from Shopify.
     */
    public String exchangeCodeForToken(String shop, String code) throws IOException {
        String url = String.format("https://%s/admin/oauth/access_token", shop);

        Map<String, String> payload = Map.of(
                "client_id", config.getClientId(),
                "client_secret", config.getClientSecret(),
                "code", code
        );

        String jsonBody = objectMapper.writeValueAsString(payload);
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("Failed to exchange code for token. Shop: {}, Status: {}", shop, response.code());
                throw new IOException("Token exchange failed with status: " + response.code());
            }

            JsonNode jsonNode = objectMapper.readTree(response.body().string());
            String accessToken = jsonNode.path("access_token").asText(null);

            if (accessToken == null || accessToken.isBlank()) {
                throw new IOException("No access_token in Shopify response");
            }

            log.info("Successfully exchanged code for access token. Shop: {}", shop);
            return accessToken;
        }
    }

    /**
     * Persists the Shopify connection (access token + shop domain) for the seller.
     */
    @Transactional
    public void saveShopifyConnection(Long sellerId, String shop, String accessToken) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalStateException("Seller not found: " + sellerId));

        // Build credentials map matching ShopifyConnector's expected format
        Map<String, String> credentials = Map.of(
                "shop_url", shop,
                "access_token", accessToken
        );

        String credentialsJson;
        try {
            credentialsJson = objectMapper.writeValueAsString(credentials);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize credentials", e);
        }

        String encryptedCredentials = encryptionService.encrypt(credentialsJson);

        // Check if a Shopify platform already exists for this seller
        List<SellerPlatform> existingPlatforms = platformRepository.findBySellerId(sellerId);
        Optional<SellerPlatform> existingShopify = existingPlatforms.stream()
                .filter(p -> p.getPlatformType() == PlatformType.SHOPIFY)
                .findFirst();

        SellerPlatform platform;
        if (existingShopify.isPresent()) {
            platform = existingShopify.get();
            log.info("Updating existing Shopify connection for seller: {}", sellerId);
        } else {
            platform = new SellerPlatform();
            platform.setSeller(seller);
            platform.setPlatformType(PlatformType.SHOPIFY);
            platform.setCreationMethod(AccountCreationMethod.EXISTING_ACCOUNT);
            log.info("Creating new Shopify connection for seller: {}", sellerId);
        }

        platform.setStoreName(shop);
        platform.setEncryptedCredentials(encryptedCredentials);
        platform.setStatus(ConnectionStatus.CONNECTED);
        platform.setExternalStoreId(shop);
        platform.setLastSyncError(null);

        SellerPlatform saved = platformRepository.save(platform);
        log.info("Shopify connection saved successfully for seller: {}, shop: {}", sellerId, shop);

        // Register after persistence so a delivery can resolve this platform by shop domain.
        // Failure is non-fatal: the connected store remains usable and the operator gets a clear log.
        webhookRegistrationService.registerOrderWebhooks(saved);

        // Import the seller's Shopify catalogue into the local products table BEFORE
        // triggering autopilot. The OAuth handshake already has the access token
        // and read_products scope, so this is the right moment to seed real data
        // — without it the Products UI, the analytics evidence and the agent's
        // apply path all run against an empty (or demo-bootstrapped) catalogue.
        // Wrapped in try/catch so a Shopify rate-limit or transient error never
        // breaks the OAuth callback.
        try {
            ShopifyCatalogueSyncService.SyncResult syncResult =
                    catalogueSyncService.syncCatalogue(saved.getId());
            if (syncResult.ok()) {
                log.info("Initial Shopify catalogue sync for platform {}: {} products / {} variants from {} fetched",
                        saved.getId(), syncResult.productsUpserted(),
                        syncResult.variantsUpserted(), syncResult.productsFetched());
            } else {
                log.warn("Initial Shopify catalogue sync for platform {} failed: {}",
                        saved.getId(), syncResult.error());
            }
        } catch (Exception e) {
            log.warn("Catalogue sync failed for seller {} after OAuth: {}", sellerId, e.getMessage());
        }

        // Kick off a first autopilot run so the Approval Queue / Impact Lab have
        // real AI-generated content immediately. Async; this returns fast. When
        // the catalogue sync above pulled real products, the engine reasons over
        // those; otherwise the orchestrator's bootstrap fallback kicks in.
        try {
            AgentRun run = autopilotOrchestrator.startRun(String.valueOf(saved.getId()), "WEBHOOK");
            autopilotOrchestrator.executeRunAsync(run);
            log.info("Triggered first autopilot run {} for newly connected store {}", run.getRunId(), saved.getId());
        } catch (Exception e) {
            log.warn("Failed to trigger initial autopilot run for seller {}: {}", sellerId, e.getMessage());
        }
    }

    private String generateSecureState() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void cleanExpiredStates() {
        long now = System.currentTimeMillis();
        stateStore.entrySet().removeIf(entry -> now - entry.getValue().createdAt() > 600_000);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Re-registers all Shopify order webhook topics for the seller's connected store.
     * Safe to call multiple times — {@code registerIfMissing} is idempotent.
     *
     * @throws IllegalStateException if no connected Shopify platform found
     */
    public Map<String, Object> reRegisterWebhooks(Long sellerId) {
        SellerPlatform platform = platformRepository.findBySellerId(sellerId)
                .stream()
                .filter(p -> p.getPlatformType() == PlatformType.SHOPIFY
                          && p.getStatus() == ConnectionStatus.CONNECTED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No connected Shopify store found for seller " + sellerId));

        boolean ok = webhookRegistrationService.registerOrderWebhooks(platform);
        String webhookUrl = config.getWebhookBaseUrl();
        boolean urlConfigured = webhookUrl != null && webhookUrl.startsWith("https://");
        return Map.of(
                "registered", ok,
                "shopUrl", platform.getStoreName() != null ? platform.getStoreName() : "unknown",
                "webhookEndpoint", urlConfigured
                        ? webhookUrl.replaceAll("/+$", "") + "/api/v1/webhooks/shopify/orders"
                        : "(SHOPIFY_WEBHOOK_BASE_URL not configured)",
                "topics", List.of("orders/create", "orders/updated", "orders/cancelled",
                        "orders/paid", "orders/fulfilled"),
                "hint", urlConfigured ? "" : "Set SHOPIFY_WEBHOOK_BASE_URL to a public HTTPS URL (e.g. ngrok) and retry."
        );
    }

    public record OAuthStateEntry(String shop, Long sellerId, long createdAt) {}
}
