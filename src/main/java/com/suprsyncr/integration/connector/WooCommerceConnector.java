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
 * WooCommerce marketplace connector implementation.
 * Handles API calls to WooCommerce REST API version 3 with OAuth 1.0a authentication.
 */
@Component
public class WooCommerceConnector implements MarketplaceConnector {
    
    private static final Logger log = LoggerFactory.getLogger(WooCommerceConnector.class);
    private static final String API_VERSION = "wc/v3";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public WooCommerceConnector(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public PlatformType getPlatformType() {
        return PlatformType.WOOCOMMERCE;
    }
    
    @Override
    public ConnectionTestResult testConnection(Map<String, String> credentials) {
        String siteUrl = credentials.get("site_url");
        String consumerKey = credentials.get("consumer_key");
        String consumerSecret = credentials.get("consumer_secret");
        
        if (siteUrl == null || consumerKey == null || consumerSecret == null) {
            return new ConnectionTestResult(false, "Missing required credentials: site_url, consumer_key, and consumer_secret", Map.of());
        }
        
        String url = buildUrl(siteUrl, "/wp-json/" + API_VERSION + "/system_status", consumerKey, consumerSecret);
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.body().string());
                String storeName = jsonNode.path("settings").path("site_title").asText(siteUrl);
                return new ConnectionTestResult(true, "Connection successful", Map.of("store_name", storeName));
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                return new ConnectionTestResult(false, "Connection failed: " + errorBody, Map.of("status_code", response.code()));
            }
        } catch (IOException e) {
            return new ConnectionTestResult(false, "Connection error: " + e.getMessage(), Map.of());
        }
    }
    
    @Override
    public Map<String, String> createSellerAccount(MarketplaceOnboardingData onboardingData) {
        // WooCommerce doesn't support automatic account creation via API
        // Sellers must set up their WooCommerce store manually (via WordPress.com hosting or self-hosted)
        // and provide API credentials
        throw new UnsupportedOperationException("WooCommerce does not support automatic account creation. " +
                "Sellers must set up a WooCommerce store manually via WordPress.com hosting or self-hosted installation " +
                "and provide API credentials (site_url, consumer_key, consumer_secret).");
    }
    
    @Override
    public String publishProduct(Product product, Map<String, String> credentials) {
        String siteUrl = credentials.get("site_url");
        String consumerKey = credentials.get("consumer_key");
        String consumerSecret = credentials.get("consumer_secret");
        
        String url = buildUrl(siteUrl, "/wp-json/" + API_VERSION + "/products", consumerKey, consumerSecret);
        
        ObjectNode productNode = buildProductPayload(product);
        
        RequestBody body = RequestBody.create(productNode.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.body().string());
                return jsonNode.path("id").asText();
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("WOOCOMMERCE", response.code(), 
                        "Failed to publish product: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("WOOCOMMERCE", null, 
                    "Error publishing product: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void updateProduct(String externalProductId, Product product, Map<String, String> credentials) {
        String siteUrl = credentials.get("site_url");
        String consumerKey = credentials.get("consumer_key");
        String consumerSecret = credentials.get("consumer_secret");
        
        String url = buildUrl(siteUrl, "/wp-json/" + API_VERSION + "/products/" + externalProductId, 
                consumerKey, consumerSecret);
        
        ObjectNode productNode = buildProductPayload(product);
        
        RequestBody body = RequestBody.create(productNode.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("WOOCOMMERCE", response.code(), 
                        "Failed to update product: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("WOOCOMMERCE", null, 
                    "Error updating product: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void delistProduct(String externalProductId, Map<String, String> credentials) {
        String siteUrl = credentials.get("site_url");
        String consumerKey = credentials.get("consumer_key");
        String consumerSecret = credentials.get("consumer_secret");
        
        String url = buildUrl(siteUrl, "/wp-json/" + API_VERSION + "/products/" + externalProductId, 
                consumerKey, consumerSecret);
        
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("WOOCOMMERCE", response.code(), 
                        "Failed to delist product: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("WOOCOMMERCE", null, 
                    "Error delisting product: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ExternalOrder> fetchOrders(Map<String, String> credentials, LocalDateTime since) {
        String siteUrl = credentials.get("site_url");
        String consumerKey = credentials.get("consumer_key");
        String consumerSecret = credentials.get("consumer_secret");
        
        String after = since.format(DateTimeFormatter.ISO_DATE_TIME);
        String url = buildUrl(siteUrl, "/wp-json/" + API_VERSION + "/orders?after=" + after, 
                consumerKey, consumerSecret);
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.body().string());
                return parseOrders(jsonNode);
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("WOOCOMMERCE", response.code(), 
                        "Failed to fetch orders: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("WOOCOMMERCE", null, 
                    "Error fetching orders: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void updateOrderStatus(String externalOrderId, String status, Map<String, String> credentials) {
        String siteUrl = credentials.get("site_url");
        String consumerKey = credentials.get("consumer_key");
        String consumerSecret = credentials.get("consumer_secret");
        
        String url = buildUrl(siteUrl, "/wp-json/" + API_VERSION + "/orders/" + externalOrderId, 
                consumerKey, consumerSecret);
        
        ObjectNode statusNode = objectMapper.createObjectNode();
        statusNode.put("status", status);
        
        RequestBody body = RequestBody.create(statusNode.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("WOOCOMMERCE", response.code(), 
                        "Failed to update order status: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("WOOCOMMERCE", null, 
                    "Error updating order status: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void updateTracking(String externalOrderId, String trackingNumber, String courier, Map<String, String> credentials) {
        String siteUrl = credentials.get("site_url");
        String consumerKey = credentials.get("consumer_key");
        String consumerSecret = credentials.get("consumer_secret");
        
        String url = buildUrl(siteUrl, "/wp-json/" + API_VERSION + "/orders/" + externalOrderId, 
                consumerKey, consumerSecret);
        
        ObjectNode metaDataNode = objectMapper.createObjectNode();
        var metaArray = objectMapper.createArrayNode();
        
        ObjectNode trackingMeta = objectMapper.createObjectNode();
        trackingMeta.put("key", "_tracking_number");
        trackingMeta.put("value", trackingNumber);
        metaArray.add(trackingMeta);
        
        ObjectNode courierMeta = objectMapper.createObjectNode();
        courierMeta.put("key", "_courier_partner");
        courierMeta.put("value", courier);
        metaArray.add(courierMeta);
        
        metaDataNode.set("meta_data", metaArray);
        
        RequestBody body = RequestBody.create(metaDataNode.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("WOOCOMMERCE", response.code(), 
                        "Failed to update tracking: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("WOOCOMMERCE", null, 
                    "Error updating tracking: " + e.getMessage(), e);
        }
    }
    
    /**
     * Builds WooCommerce API URL with OAuth 1.0a authentication parameters.
     * WooCommerce uses consumer_key and consumer_secret as query parameters for authentication.
     */
    private String buildUrl(String siteUrl, String path, String consumerKey, String consumerSecret) {
        // Remove trailing slash from site URL if present
        if (siteUrl.endsWith("/")) {
            siteUrl = siteUrl.substring(0, siteUrl.length() - 1);
        }
        
        // Build URL with authentication parameters
        String separator = path.contains("?") ? "&" : "?";
        return siteUrl + path + separator + "consumer_key=" + consumerKey + "&consumer_secret=" + consumerSecret;
    }
    
    /**
     * Builds WooCommerce product payload from Product entity.
     */
    private ObjectNode buildProductPayload(Product product) {
        ObjectNode root = objectMapper.createObjectNode();
        
        root.put("name", product.getName());
        root.put("description", product.getDescription() != null ? product.getDescription() : "");
        root.put("sku", product.getSku());
        root.put("regular_price", product.getBasePrice().toString());
        root.put("status", "publish");
        
        // Add brand as meta data
        if (product.getBrand() != null) {
            var metaArray = objectMapper.createArrayNode();
            ObjectNode brandMeta = objectMapper.createObjectNode();
            brandMeta.put("key", "_brand");
            brandMeta.put("value", product.getBrand());
            metaArray.add(brandMeta);
            root.set("meta_data", metaArray);
        }
        
        // Add dimensions
        if (product.getWeight() != null || product.getLength() != null || 
            product.getWidth() != null || product.getHeight() != null) {
            ObjectNode dimensions = objectMapper.createObjectNode();
            if (product.getWeight() != null) {
                root.put("weight", product.getWeight().toString());
            }
            if (product.getLength() != null) {
                dimensions.put("length", product.getLength().toString());
            }
            if (product.getWidth() != null) {
                dimensions.put("width", product.getWidth().toString());
            }
            if (product.getHeight() != null) {
                dimensions.put("height", product.getHeight().toString());
            }
            root.set("dimensions", dimensions);
        }
        
        // Add images
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            var imagesArray = objectMapper.createArrayNode();
            for (String imageUrl : product.getImageUrls()) {
                ObjectNode imageNode = objectMapper.createObjectNode();
                imageNode.put("src", imageUrl);
                imagesArray.add(imageNode);
            }
            root.set("images", imagesArray);
        }
        
        // Add variants as product variations
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            var attributesArray = objectMapper.createArrayNode();
            var variationsArray = objectMapper.createArrayNode();
            
            for (ProductVariant variant : product.getVariants()) {
                ObjectNode variationNode = objectMapper.createObjectNode();
                variationNode.put("sku", variant.getSku());
                variationNode.put("regular_price", variant.getPrice().toString());
                
                if (variant.getImageUrl() != null) {
                    ObjectNode imageNode = objectMapper.createObjectNode();
                    imageNode.put("src", variant.getImageUrl());
                    variationNode.set("image", imageNode);
                }
                
                variationsArray.add(variationNode);
            }
            
            root.set("variations", variationsArray);
        }
        
        return root;
    }
    
    /**
     * Parses WooCommerce orders JSON response into ExternalOrder list.
     */
    private List<ExternalOrder> parseOrders(JsonNode jsonNode) {
        List<ExternalOrder> orders = new ArrayList<>();
        
        if (jsonNode.isArray()) {
            for (JsonNode orderNode : jsonNode) {
                String externalOrderId = orderNode.path("id").asText();
                BigDecimal totalAmount = new BigDecimal(orderNode.path("total").asText("0"));
                
                JsonNode billing = orderNode.path("billing");
                String customerName = billing.path("first_name").asText("") + " " + billing.path("last_name").asText("");
                String customerEmail = billing.path("email").asText("");
                String customerPhone = billing.path("phone").asText("");
                
                JsonNode shipping = orderNode.path("shipping");
                String address = String.format("%s, %s, %s %s", 
                        shipping.path("address_1").asText(""),
                        shipping.path("city").asText(""),
                        shipping.path("state").asText(""),
                        shipping.path("postcode").asText(""));
                
                String createdAt = orderNode.path("date_created_gmt").asText();
                LocalDateTime orderedAt = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_DATE_TIME);
                
                // Parse line items
                List<ExternalOrderItem> items = new ArrayList<>();
                JsonNode lineItems = orderNode.path("line_items");
                if (lineItems.isArray()) {
                    for (JsonNode itemNode : lineItems) {
                        String productId = itemNode.path("product_id").asText();
                        String productName = itemNode.path("name").asText();
                        String variantName = itemNode.path("variation_id").asText("");
                        Integer quantity = itemNode.path("quantity").asInt();
                        BigDecimal unitPrice = new BigDecimal(itemNode.path("price").asText("0"));
                        
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
