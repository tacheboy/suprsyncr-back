package com.suprsyncr.order.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.common.dto.ApiResponse;
import com.suprsyncr.common.dto.ErrorResponse;
import com.suprsyncr.integration.connector.ConnectorRegistry;
import com.suprsyncr.integration.connector.ExternalOrder;
import com.suprsyncr.integration.connector.ExternalOrderItem;
import com.suprsyncr.integration.connector.WebhookValidator;
import com.suprsyncr.order.dto.OrderDto;
import com.suprsyncr.order.service.OrderService;
import com.suprsyncr.seller.entity.PlatformType;
import com.suprsyncr.seller.entity.SellerPlatform;
import com.suprsyncr.seller.repository.SellerPlatformRepository;
import com.suprsyncr.seller.service.CredentialEncryptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling webhook callbacks from marketplace platforms.
 * Validates webhook signatures and ingests orders into the USP system.
 * 
 * Requirements: 14, 45, 96
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Webhook endpoints for receiving orders from marketplace platforms")
public class WebhookController {
    
    private final OrderService orderService;
    private final ConnectorRegistry connectorRegistry;
    private final SellerPlatformRepository platformRepository;
    private final CredentialEncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    
    public WebhookController(
            OrderService orderService,
            ConnectorRegistry connectorRegistry,
            SellerPlatformRepository platformRepository,
            CredentialEncryptionService encryptionService,
            ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.connectorRegistry = connectorRegistry;
        this.platformRepository = platformRepository;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handles Shopify order webhooks.
     * Validates X-Shopify-Hmac-SHA256 signature and ingests the order.
     * 
     * @param signature the HMAC-SHA256 signature from the header
     * @param payload the raw webhook payload
     * @return 200 OK if successful, 401 if signature is invalid
     */
    @PostMapping("/shopify/orders")
    @Operation(summary = "Shopify order webhook", description = "Receives and processes order webhooks from Shopify")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order ingested successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid webhook signature"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> handleShopifyOrder(
            @RequestHeader("X-Shopify-Hmac-SHA256") String signature,
            @RequestBody String payload) {
        
        return handleWebhook(PlatformType.SHOPIFY, signature, payload);
    }
    
    /**
     * Handles Blinkit order webhooks.
     * Validates X-Blinkit-Signature and ingests the order.
     * 
     * @param signature the signature from the header
     * @param payload the raw webhook payload
     * @return 200 OK if successful, 401 if signature is invalid
     */
    @PostMapping("/blinkit/orders")
    @Operation(summary = "Blinkit order webhook", description = "Receives and processes order webhooks from Blinkit")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order ingested successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid webhook signature"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> handleBlinkitOrder(
            @RequestHeader("X-Blinkit-Signature") String signature,
            @RequestBody String payload) {
        
        return handleWebhook(PlatformType.BLINKIT, signature, payload);
    }
    
    /**
     * Handles WooCommerce order webhooks.
     * Validates X-WC-Webhook-Signature and ingests the order.
     * 
     * @param signature the signature from the header
     * @param payload the raw webhook payload
     * @return 200 OK if successful, 401 if signature is invalid
     */
    @PostMapping("/woocommerce/orders")
    @Operation(summary = "WooCommerce order webhook", description = "Receives and processes order webhooks from WooCommerce")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order ingested successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid webhook signature"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> handleWooCommerceOrder(
            @RequestHeader("X-WC-Webhook-Signature") String signature,
            @RequestBody String payload) {
        
        return handleWebhook(PlatformType.WOOCOMMERCE, signature, payload);
    }
    
    /**
     * Common webhook handling logic for all platforms.
     * Validates signature, parses order data, and ingests the order.
     */
    private ResponseEntity<?> handleWebhook(
            PlatformType platformType,
            String signature,
            String payload) {
        
        try {
            // Parse the payload to extract external order ID
            JsonNode orderJson = objectMapper.readTree(payload);
            String externalOrderId = extractExternalOrderId(orderJson, platformType);
            
            // Find the platform connection by external order ID or store identifier
            SellerPlatform platform = findPlatformForWebhook(orderJson, platformType);
            
            if (platform == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.of("Unauthorized", "Platform connection not found", 401));
            }
            
            // Decrypt credentials to get the webhook secret
            String decryptedCredentials = encryptionService.decrypt(platform.getEncryptedCredentials());
            Map<String, String> credentials = parseCredentials(decryptedCredentials);
            String webhookSecret = getWebhookSecret(credentials, platformType);
            
            // Validate the webhook signature
            WebhookValidator validator = connectorRegistry.getValidator(platformType);
            boolean isValid = validator.validateSignature(payload, signature, webhookSecret);
            
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.of("Unauthorized", "Invalid webhook signature", 401));
            }
            
            // Parse the order data
            ExternalOrder externalOrder = parseOrderData(orderJson, platformType);
            
            // Ingest the order
            OrderDto orderDto = orderService.ingestOrder(platform.getId(), externalOrder);
            
            return ResponseEntity.ok(ApiResponse.success(orderDto));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.of("Internal Server Error", "Failed to process webhook: " + e.getMessage(), 500));
        }
    }
    
    /**
     * Extracts the external order ID from the webhook payload.
     */
    private String extractExternalOrderId(JsonNode orderJson, PlatformType platformType) {
        return switch (platformType) {
            case SHOPIFY -> orderJson.path("id").asText();
            case BLINKIT -> orderJson.path("order_id").asText();
            case WOOCOMMERCE -> orderJson.path("id").asText();
        };
    }
    
    /**
     * Finds the platform connection for the webhook.
     * Uses store identifier from the payload to match against platform connections.
     */
    private SellerPlatform findPlatformForWebhook(JsonNode orderJson, PlatformType platformType) {
        String storeIdentifier = extractStoreIdentifier(orderJson, platformType);
        
        List<SellerPlatform> platforms = platformRepository.findAll();
        for (SellerPlatform platform : platforms) {
            if (platform.getPlatformType() == platformType) {
                // Match by external store ID or store name
                if (storeIdentifier.equals(platform.getExternalStoreId()) ||
                    storeIdentifier.equals(platform.getStoreName())) {
                    return platform;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extracts the store identifier from the webhook payload.
     */
    private String extractStoreIdentifier(JsonNode orderJson, PlatformType platformType) {
        return switch (platformType) {
            case SHOPIFY -> orderJson.path("shop_domain").asText();
            case BLINKIT -> orderJson.path("seller_id").asText();
            case WOOCOMMERCE -> orderJson.path("store_url").asText();
        };
    }
    
    /**
     * Parses the decrypted credentials JSON string into a map.
     */
    private Map<String, String> parseCredentials(String credentialsJson) {
        try {
            return objectMapper.readValue(credentialsJson, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse credentials", e);
        }
    }
    
    /**
     * Extracts the webhook secret from the credentials map.
     */
    private String getWebhookSecret(Map<String, String> credentials, PlatformType platformType) {
        return switch (platformType) {
            case SHOPIFY -> credentials.get("api_secret");
            case BLINKIT -> credentials.get("api_secret");
            case WOOCOMMERCE -> credentials.get("webhook_secret");
        };
    }
    
    /**
     * Parses the order data from the webhook payload into an ExternalOrder.
     */
    private ExternalOrder parseOrderData(JsonNode orderJson, PlatformType platformType) {
        return switch (platformType) {
            case SHOPIFY -> parseShopifyOrder(orderJson);
            case BLINKIT -> parseBlinkitOrder(orderJson);
            case WOOCOMMERCE -> parseWooCommerceOrder(orderJson);
        };
    }
    
    /**
     * Parses a Shopify order webhook payload.
     */
    private ExternalOrder parseShopifyOrder(JsonNode orderJson) {
        String externalOrderId = orderJson.path("id").asText();
        BigDecimal totalAmount = new BigDecimal(orderJson.path("total_price").asText());
        String customerName = orderJson.path("customer").path("first_name").asText() + " " +
                              orderJson.path("customer").path("last_name").asText();
        
        JsonNode shippingAddress = orderJson.path("shipping_address");
        String address = String.format("%s, %s, %s, %s %s",
                shippingAddress.path("address1").asText(),
                shippingAddress.path("city").asText(),
                shippingAddress.path("province").asText(),
                shippingAddress.path("country").asText(),
                shippingAddress.path("zip").asText());
        
        String customerPhone = orderJson.path("customer").path("phone").asText();
        String customerEmail = orderJson.path("customer").path("email").asText();
        LocalDateTime orderedAt = LocalDateTime.parse(
                orderJson.path("created_at").asText(),
                DateTimeFormatter.ISO_DATE_TIME);
        
        List<ExternalOrderItem> items = new ArrayList<>();
        JsonNode lineItems = orderJson.path("line_items");
        for (JsonNode item : lineItems) {
            items.add(new ExternalOrderItem(
                    item.path("product_id").asText(),
                    item.path("name").asText(),
                    item.path("variant_title").asText(),
                    item.path("quantity").asInt(),
                    new BigDecimal(item.path("price").asText())
            ));
        }
        
        return new ExternalOrder(
                externalOrderId,
                totalAmount,
                customerName,
                address,
                customerPhone,
                customerEmail,
                orderedAt,
                items
        );
    }
    
    /**
     * Parses a Blinkit order webhook payload.
     */
    private ExternalOrder parseBlinkitOrder(JsonNode orderJson) {
        String externalOrderId = orderJson.path("order_id").asText();
        BigDecimal totalAmount = new BigDecimal(orderJson.path("total_amount").asText());
        String customerName = orderJson.path("customer_name").asText();
        String address = orderJson.path("delivery_address").asText();
        String customerPhone = orderJson.path("customer_phone").asText();
        String customerEmail = orderJson.path("customer_email").asText();
        LocalDateTime orderedAt = LocalDateTime.parse(
                orderJson.path("ordered_at").asText(),
                DateTimeFormatter.ISO_DATE_TIME);
        
        List<ExternalOrderItem> items = new ArrayList<>();
        JsonNode itemsArray = orderJson.path("items");
        for (JsonNode item : itemsArray) {
            items.add(new ExternalOrderItem(
                    item.path("product_id").asText(),
                    item.path("product_name").asText(),
                    item.path("variant_name").asText(),
                    item.path("quantity").asInt(),
                    new BigDecimal(item.path("unit_price").asText())
            ));
        }
        
        return new ExternalOrder(
                externalOrderId,
                totalAmount,
                customerName,
                address,
                customerPhone,
                customerEmail,
                orderedAt,
                items
        );
    }
    
    /**
     * Parses a WooCommerce order webhook payload.
     */
    private ExternalOrder parseWooCommerceOrder(JsonNode orderJson) {
        String externalOrderId = orderJson.path("id").asText();
        BigDecimal totalAmount = new BigDecimal(orderJson.path("total").asText());
        
        JsonNode billing = orderJson.path("billing");
        String customerName = billing.path("first_name").asText() + " " +
                              billing.path("last_name").asText();
        
        JsonNode shipping = orderJson.path("shipping");
        String address = String.format("%s, %s, %s, %s %s",
                shipping.path("address_1").asText(),
                shipping.path("city").asText(),
                shipping.path("state").asText(),
                shipping.path("country").asText(),
                shipping.path("postcode").asText());
        
        String customerPhone = billing.path("phone").asText();
        String customerEmail = billing.path("email").asText();
        LocalDateTime orderedAt = LocalDateTime.parse(
                orderJson.path("date_created").asText(),
                DateTimeFormatter.ISO_DATE_TIME);
        
        List<ExternalOrderItem> items = new ArrayList<>();
        JsonNode lineItems = orderJson.path("line_items");
        for (JsonNode item : lineItems) {
            items.add(new ExternalOrderItem(
                    item.path("product_id").asText(),
                    item.path("name").asText(),
                    item.path("variation_id").asText(),
                    item.path("quantity").asInt(),
                    new BigDecimal(item.path("price").asText())
            ));
        }
        
        return new ExternalOrder(
                externalOrderId,
                totalAmount,
                customerName,
                address,
                customerPhone,
                customerEmail,
                orderedAt,
                items
        );
    }
}

