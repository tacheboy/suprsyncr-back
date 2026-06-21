package com.suprsyncr.integration.shopify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suprsyncr.integration.shopify.dto.CreateTestOrderRequest;
import com.suprsyncr.seller.entity.PlatformType;
import com.suprsyncr.seller.entity.SellerPlatform;
import com.suprsyncr.seller.service.CredentialEncryptionService;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class ShopifyTestOrderService {

    private static final String API_VERSION = "2026-04";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final CredentialEncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public ShopifyTestOrderService(CredentialEncryptionService encryptionService,
                                   ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public Map<String, Object> createTestOrder(SellerPlatform platform, CreateTestOrderRequest input) {
        if (platform.getPlatformType() != PlatformType.SHOPIFY) {
            throw new IllegalArgumentException("Test orders are supported for Shopify platforms only");
        }
        try {
            Map<String, String> credentials = objectMapper.readValue(
                    encryptionService.decrypt(platform.getEncryptedCredentials()), Map.class);
            String shop = credentials.get("shop_url");
            String token = credentials.get("access_token");
            if (shop == null || token == null) throw new IllegalStateException("Shopify credentials are incomplete");

            ObjectNode lineItem = objectMapper.createObjectNode();
            lineItem.put("title", input.productTitle());
            lineItem.put("price", input.unitPrice());
            lineItem.put("quantity", input.quantity());

            ObjectNode order = objectMapper.createObjectNode();
            order.put("test", true);
            order.put("financial_status", "paid");
            if (input.customerEmail() != null && !input.customerEmail().isBlank()) order.put("email", input.customerEmail());
            order.putArray("line_items").add(lineItem);
            ObjectNode body = objectMapper.createObjectNode();
            body.set("order", order);

            Request request = new Request.Builder()
                    .url("https://" + shop + "/admin/api/" + API_VERSION + "/orders.json")
                    .header("X-Shopify-Access-Token", token)
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) throw new IOException("Shopify test order failed (" + response.code() + "): " + responseBody);
                JsonNode created = objectMapper.readTree(responseBody).path("order");
                return Map.of(
                        "shopifyOrderId", created.path("id").asText(),
                        "orderName", created.path("name").asText(),
                        "test", created.path("test").asBoolean(true),
                        "webhookExpected", true
                );
            }
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
