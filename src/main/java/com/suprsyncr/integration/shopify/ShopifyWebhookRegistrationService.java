package com.suprsyncr.integration.shopify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suprsyncr.seller.entity.SellerPlatform;
import com.suprsyncr.seller.service.CredentialEncryptionService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Registers the order activity topics needed by the seller notification feed. */
@Service
public class ShopifyWebhookRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ShopifyWebhookRegistrationService.class);
    private static final String API_VERSION = "2026-04";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final List<String> ORDER_TOPICS = List.of(
            "orders/create", "orders/updated", "orders/cancelled", "orders/paid", "orders/fulfilled");

    private final ShopifyOAuthConfig config;
    private final CredentialEncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public ShopifyWebhookRegistrationService(ShopifyOAuthConfig config,
                                              CredentialEncryptionService encryptionService,
                                              ObjectMapper objectMapper,
                                              OkHttpClient httpClient) {
        this.config = config;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public boolean registerOrderWebhooks(SellerPlatform platform) {
        String callbackBase = config.getWebhookBaseUrl();
        if (callbackBase == null || callbackBase.isBlank()) {
            log.warn("Shopify webhooks not registered for platform {}: SHOPIFY_WEBHOOK_BASE_URL is unset", platform.getId());
            return false;
        }
        if (!callbackBase.startsWith("https://")) {
            log.warn("Shopify webhooks not registered for platform {}: callback URL must be public HTTPS", platform.getId());
            return false;
        }

        try {
            Map<String, String> credentials = objectMapper.readValue(
                    encryptionService.decrypt(platform.getEncryptedCredentials()), Map.class);
            String shop = credentials.get("shop_url");
            String accessToken = credentials.get("access_token");
            if (shop == null || accessToken == null) {
                log.warn("Shopify webhooks not registered for platform {}: missing shop_url or access_token", platform.getId());
                return false;
            }

            String address = callbackBase.replaceAll("/+$", "") + "/api/v1/webhooks/shopify/orders";
            for (String topic : ORDER_TOPICS) registerIfMissing(shop, accessToken, topic, address);
            return true;
        } catch (Exception e) {
            log.warn("Shopify webhook registration failed for platform {}: {}", platform.getId(), e.getMessage());
            return false;
        }
    }

    private void registerIfMissing(String shop, String accessToken, String topic, String address) throws IOException {
        String baseUrl = "https://" + shop + "/admin/api/" + API_VERSION + "/webhooks.json";
        if (alreadyRegistered(baseUrl, accessToken, topic, address)) return;

        ObjectNode webhook = objectMapper.createObjectNode();
        webhook.put("topic", topic);
        webhook.put("address", address);
        webhook.put("format", "json");
        ObjectNode body = objectMapper.createObjectNode();
        body.set("webhook", webhook);

        Request request = new Request.Builder().url(baseUrl)
                .header("X-Shopify-Access-Token", accessToken)
                .post(RequestBody.create(body.toString(), JSON)).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String detail = response.body() == null ? "" : response.body().string();
                throw new IOException("Shopify rejected " + topic + " webhook (" + response.code() + "): " + detail);
            }
            log.info("Registered Shopify webhook {} -> {}", topic, address);
        }
    }

    private boolean alreadyRegistered(String url, String accessToken, String topic, String address) throws IOException {
        Request request = new Request.Builder().url(url).header("X-Shopify-Access-Token", accessToken).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return false;
            JsonNode webhooks = objectMapper.readTree(response.body().string()).path("webhooks");
            for (JsonNode webhook : webhooks) {
                if (topic.equals(webhook.path("topic").asText()) && address.equals(webhook.path("address").asText())) return true;
            }
            return false;
        }
    }
}
