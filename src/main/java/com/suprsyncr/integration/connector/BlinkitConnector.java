package com.suprsyncr.integration.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suprsyncr.common.exception.MarketplaceApiException;
import com.suprsyncr.product.entity.Product;
import com.suprsyncr.product.entity.ProductVariant;
import com.suprsyncr.seller.dto.MarketplaceOnboardingData;
import com.suprsyncr.seller.entity.PlatformType;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Blinkit marketplace connector implementation.
 * Handles API calls to Blinkit API and supports automatic seller account creation.
 */
@Component
public class BlinkitConnector implements MarketplaceConnector {
    
    private static final Logger log = LoggerFactory.getLogger(BlinkitConnector.class);
    private static final String BASE_URL = "https://api.blinkit.com/v1";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public BlinkitConnector(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public PlatformType getPlatformType() {
        return PlatformType.BLINKIT;
    }
    
    @Override
    public ConnectionTestResult testConnection(Map<String, String> credentials) {
        String sellerId = credentials.get("seller_id");
        log.info("Testing Blinkit connection for seller: {}", sellerId);
        
        String apiKey = credentials.get("api_key");
        String apiSecret = credentials.get("api_secret");
        
        if (sellerId == null || apiKey == null || apiSecret == null) {
            log.warn("Blinkit connection test failed - missing credentials");
            return new ConnectionTestResult(false, "Missing required credentials: seller_id, api_key, and api_secret", Map.of());
        }
        
        String url = BASE_URL + "/seller/profile";
        
        Request request = new Request.Builder()
                .url(url)
                .header("X-API-Key", apiKey)
                .header("X-API-Secret", apiSecret)
                .header("X-Seller-Id", sellerId)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.body().string());
                String storeName = jsonNode.path("data").path("store_name").asText();
                log.info("Blinkit connection test successful for seller: {}", sellerId);
                return new ConnectionTestResult(true, "Connection successful", Map.of("store_name", storeName));
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                log.error("Blinkit connection test failed for seller: {} - Status: {}", sellerId, response.code());
                return new ConnectionTestResult(false, "Connection failed: " + errorBody, Map.of("status_code", response.code()));
            }
        } catch (IOException e) {
            log.error("Blinkit connection test error for seller: {}", sellerId, e);
            return new ConnectionTestResult(false, "Connection error: " + e.getMessage(), Map.of());
        }
    }
    
    @Override
    public Map<String, String> createSellerAccount(MarketplaceOnboardingData onboardingData) {
        log.info("Creating Blinkit seller account for business: {}", onboardingData.businessName());
        
        String url = BASE_URL + "/seller/onboard";
        
        ObjectNode requestBody = buildOnboardingPayload(onboardingData);
        
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.body().string());
                JsonNode data = jsonNode.path("data");
                
                Map<String, String> credentials = new HashMap<>();
                credentials.put("seller_id", data.path("seller_id").asText());
                credentials.put("api_key", data.path("api_key").asText());
                credentials.put("api_secret", data.path("api_secret").asText());
                
                log.info("Blinkit seller account created successfully for business: {}, SellerID: {}", 
                        onboardingData.businessName(), credentials.get("seller_id"));
                return credentials;
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                log.error("Failed to create Blinkit seller account for business: {} - Status: {}", 
                        onboardingData.businessName(), response.code());
                throw new MarketplaceApiException("BLINKIT", response.code(), 
                        "Failed to create seller account: " + errorBody);
            }
        } catch (IOException e) {
            log.error("Error creating Blinkit seller account for business: {}", onboardingData.businessName(), e);
            throw new MarketplaceApiException("BLINKIT", null, 
                    "Error creating seller account: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String publishProduct(Product product, Map<String, String> credentials) {
        String url = BASE_URL + "/products";
        
        ObjectNode productNode = buildProductPayload(product);
        
        RequestBody body = RequestBody.create(productNode.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("X-API-Key", credentials.get("api_key"))
                .header("X-API-Secret", credentials.get("api_secret"))
                .header("X-Seller-Id", credentials.get("seller_id"))
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.body().string());
                return jsonNode.path("data").path("product_id").asText();
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("BLINKIT", response.code(), 
                        "Failed to publish product: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("BLINKIT", null, 
                    "Error publishing product: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void updateProduct(String externalProductId, Product product, Map<String, String> credentials) {
        String url = BASE_URL + "/products/" + externalProductId;
        
        ObjectNode productNode = buildProductPayload(product);
        
        RequestBody body = RequestBody.create(productNode.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("X-API-Key", credentials.get("api_key"))
                .header("X-API-Secret", credentials.get("api_secret"))
                .header("X-Seller-Id", credentials.get("seller_id"))
                .put(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("BLINKIT", response.code(), 
                        "Failed to update product: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("BLINKIT", null, 
                    "Error updating product: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void delistProduct(String externalProductId, Map<String, String> credentials) {
        String url = BASE_URL + "/products/" + externalProductId;
        
        Request request = new Request.Builder()
                .url(url)
                .header("X-API-Key", credentials.get("api_key"))
                .header("X-API-Secret", credentials.get("api_secret"))
                .header("X-Seller-Id", credentials.get("seller_id"))
                .delete()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("BLINKIT", response.code(), 
                        "Failed to delist product: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("BLINKIT", null, 
                    "Error delisting product: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ExternalOrder> fetchOrders(Map<String, String> credentials, LocalDateTime since) {
        String createdAfter = since.format(DateTimeFormatter.ISO_DATE_TIME);
        String url = BASE_URL + "/orders?created_after=" + createdAfter;
        
        Request request = new Request.Builder()
                .url(url)
                .header("X-API-Key", credentials.get("api_key"))
                .header("X-API-Secret", credentials.get("api_secret"))
                .header("X-Seller-Id", credentials.get("seller_id"))
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.body().string());
                return parseOrders(jsonNode);
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("BLINKIT", response.code(), 
                        "Failed to fetch orders: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("BLINKIT", null, 
                    "Error fetching orders: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void updateOrderStatus(String externalOrderId, String status, Map<String, String> credentials) {
        String url = BASE_URL + "/orders/" + externalOrderId + "/status";
        
        ObjectNode statusNode = objectMapper.createObjectNode();
        statusNode.put("status", status);
        
        RequestBody body = RequestBody.create(statusNode.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("X-API-Key", credentials.get("api_key"))
                .header("X-API-Secret", credentials.get("api_secret"))
                .header("X-Seller-Id", credentials.get("seller_id"))
                .patch(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("BLINKIT", response.code(), 
                        "Failed to update order status: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("BLINKIT", null, 
                    "Error updating order status: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void updateTracking(String externalOrderId, String trackingNumber, String courier, Map<String, String> credentials) {
        String url = BASE_URL + "/orders/" + externalOrderId + "/tracking";
        
        ObjectNode trackingNode = objectMapper.createObjectNode();
        trackingNode.put("tracking_number", trackingNumber);
        trackingNode.put("courier_partner", courier);
        
        RequestBody body = RequestBody.create(trackingNode.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("X-API-Key", credentials.get("api_key"))
                .header("X-API-Secret", credentials.get("api_secret"))
                .header("X-Seller-Id", credentials.get("seller_id"))
                .patch(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("BLINKIT", response.code(), 
                        "Failed to update tracking: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("BLINKIT", null, 
                    "Error updating tracking: " + e.getMessage(), e);
        }
    }
    
    /**
     * Builds Blinkit onboarding payload from MarketplaceOnboardingData.
     */
    private ObjectNode buildOnboardingPayload(MarketplaceOnboardingData onboardingData) {
        ObjectNode root = objectMapper.createObjectNode();
        
        root.put("business_name", onboardingData.businessName());
        root.put("owner_name", onboardingData.ownerName());
        root.put("email", onboardingData.email());
        root.put("phone_number", onboardingData.phoneNumber());
        root.put("gstin", onboardingData.gstin());
        root.put("business_address", onboardingData.businessAddress());
        root.put("city", onboardingData.city());
        root.put("state", onboardingData.state());
        root.put("pincode", onboardingData.pincode());
        
        // Bank details
        ObjectNode bankDetails = objectMapper.createObjectNode();
        bankDetails.put("account_number", onboardingData.bankAccountNumber());
        bankDetails.put("ifsc_code", onboardingData.bankIfscCode());
        bankDetails.put("account_holder_name", onboardingData.bankAccountHolderName());
        root.set("bank_details", bankDetails);
        
        // Primary warehouse
        if (onboardingData.primaryWarehouse() != null) {
            ObjectNode warehouse = objectMapper.createObjectNode();
            warehouse.put("name", onboardingData.primaryWarehouse().name());
            warehouse.put("address", onboardingData.primaryWarehouse().address());
            warehouse.put("city", onboardingData.primaryWarehouse().city());
            warehouse.put("state", onboardingData.primaryWarehouse().state());
            warehouse.put("pincode", onboardingData.primaryWarehouse().pincode());
            root.set("primary_warehouse", warehouse);
        }
        
        return root;
    }
    
    /**
     * Builds Blinkit product payload from Product entity.
     */
    private ObjectNode buildProductPayload(Product product) {
        ObjectNode root = objectMapper.createObjectNode();
        
        root.put("name", product.getName());
        root.put("description", product.getDescription());
        root.put("sku", product.getSku());
        root.put("base_price", product.getBasePrice().toString());
        root.put("brand", product.getBrand() != null ? product.getBrand() : "");
        
        // Add dimensions
        if (product.getWeight() != null) {
            root.put("weight_grams", product.getWeight().toString());
        }
        if (product.getLength() != null && product.getWidth() != null && product.getHeight() != null) {
            ObjectNode dimensions = objectMapper.createObjectNode();
            dimensions.put("length_cm", product.getLength().toString());
            dimensions.put("width_cm", product.getWidth().toString());
            dimensions.put("height_cm", product.getHeight().toString());
            root.set("dimensions", dimensions);
        }
        
        // Add variants
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            var variantsArray = objectMapper.createArrayNode();
            for (ProductVariant variant : product.getVariants()) {
                ObjectNode variantNode = objectMapper.createObjectNode();
                variantNode.put("sku", variant.getSku());
                variantNode.put("name", variant.getVariantName());
                variantNode.put("price", variant.getPrice().toString());
                if (variant.getImageUrl() != null) {
                    variantNode.put("image_url", variant.getImageUrl());
                }
                variantsArray.add(variantNode);
            }
            root.set("variants", variantsArray);
        }
        
        // Add images
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            var imagesArray = objectMapper.createArrayNode();
            for (String imageUrl : product.getImageUrls()) {
                imagesArray.add(imageUrl);
            }
            root.set("images", imagesArray);
        }
        
        return root;
    }
    
    /**
     * Parses Blinkit orders JSON response into ExternalOrder list.
     */
    private List<ExternalOrder> parseOrders(JsonNode jsonNode) {
        List<ExternalOrder> orders = new ArrayList<>();
        JsonNode ordersArray = jsonNode.path("data").path("orders");
        
        if (ordersArray.isArray()) {
            for (JsonNode orderNode : ordersArray) {
                String externalOrderId = orderNode.path("order_id").asText();
                BigDecimal totalAmount = new BigDecimal(orderNode.path("total_amount").asText("0"));
                
                JsonNode customer = orderNode.path("customer");
                String customerName = customer.path("name").asText("");
                String customerEmail = customer.path("email").asText("");
                String customerPhone = customer.path("phone").asText("");
                
                JsonNode deliveryAddress = orderNode.path("delivery_address");
                String address = String.format("%s, %s, %s %s", 
                        deliveryAddress.path("street").asText(""),
                        deliveryAddress.path("city").asText(""),
                        deliveryAddress.path("state").asText(""),
                        deliveryAddress.path("pincode").asText(""));
                
                String createdAt = orderNode.path("created_at").asText();
                LocalDateTime orderedAt = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_DATE_TIME);
                
                // Parse items
                List<ExternalOrderItem> items = new ArrayList<>();
                JsonNode itemsArray = orderNode.path("items");
                if (itemsArray.isArray()) {
                    for (JsonNode itemNode : itemsArray) {
                        String productId = itemNode.path("product_id").asText();
                        String productName = itemNode.path("product_name").asText();
                        String variantName = itemNode.path("variant_name").asText("");
                        Integer quantity = itemNode.path("quantity").asInt();
                        BigDecimal unitPrice = new BigDecimal(itemNode.path("unit_price").asText("0"));
                        
                        items.add(new ExternalOrderItem(productId, productName, variantName, quantity, unitPrice));
                    }
                }
                
                orders.add(new ExternalOrder(externalOrderId, totalAmount, customerName, 
                        address, customerPhone, customerEmail, orderedAt, items));
            }
        }
        
        return orders;
    }
}
