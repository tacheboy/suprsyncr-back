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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shopify marketplace connector implementation.
 * Handles API calls to Shopify Admin API version 2024-01.
 *
 * MULTI-PLATFORM NOTE:
 * This connector implements the core PlatformGateway concept for Autopilot.
 * To integrate WooCommerce, Magento, etc., create a new implementation of MarketplaceConnector
 * (e.g. WooCommerceConnector) and map generic ProposedChange payloads to the platform's specific API.
 * See multi_platform_integration_guide.md for detailed developer instructions.
 */
@Component
public class ShopifyConnector implements MarketplaceConnector {
    
    private static final Logger log = LoggerFactory.getLogger(ShopifyConnector.class);
    private static final String API_VERSION = "2024-01";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public ShopifyConnector(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public PlatformType getPlatformType() {
        return PlatformType.SHOPIFY;
    }
    
    @Override
    public ConnectionTestResult testConnection(Map<String, String> credentials) {
        String shopUrl = credentials.get("shop_url");
        log.info("Testing Shopify connection for shop: {}", shopUrl);
        
        String accessToken = credentials.get("access_token");
        
        if (shopUrl == null || accessToken == null) {
            log.warn("Shopify connection test failed - missing credentials");
            return new ConnectionTestResult(false, "Missing required credentials: shop_url and access_token", Map.of());
        }
        
        String url = String.format("https://%s/admin/api/%s/shop.json", shopUrl, API_VERSION);
        
        Request request = new Request.Builder()
                .url(url)
                .header("X-Shopify-Access-Token", accessToken)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.body().string());
                String shopName = jsonNode.path("shop").path("name").asText();
                log.info("Shopify connection test successful for shop: {}", shopUrl);
                return new ConnectionTestResult(true, "Connection successful", Map.of("shop_name", shopName));
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                log.error("Shopify connection test failed for shop: {} - Status: {}, Error: {}", 
                        shopUrl, response.code(), errorBody);
                return new ConnectionTestResult(false, "Connection failed: " + errorBody, Map.of("status_code", response.code()));
            }
        } catch (IOException e) {
            log.error("Shopify connection test error for shop: {}", shopUrl, e);
            return new ConnectionTestResult(false, "Connection error: " + e.getMessage(), Map.of());
        }
    }
    
    @Override
    public Map<String, String> createSellerAccount(MarketplaceOnboardingData onboardingData) {
        // Shopify doesn't support automatic account creation via API
        // Sellers must create their Shopify store manually and provide credentials
        throw new UnsupportedOperationException("Shopify does not support automatic account creation. " +
                "Sellers must create a Shopify store manually and provide API credentials.");
    }
    
    @Override
    public String publishProduct(Product product, Map<String, String> credentials) {
        String shopUrl = credentials.get("shop_url");
        String accessToken = credentials.get("access_token");
        
        log.info("Publishing product to Shopify - Shop: {}, Product: {}, SKU: {}", 
                shopUrl, product.getName(), product.getSku());
        
        String url = String.format("https://%s/admin/api/%s/products.json", shopUrl, API_VERSION);
        
        ObjectNode productNode = buildProductPayload(product);
        
        RequestBody body = RequestBody.create(productNode.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("X-Shopify-Access-Token", accessToken)
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.body().string());
                String externalProductId = jsonNode.path("product").path("id").asText();
                log.info("Product published to Shopify successfully - Shop: {}, SKU: {}, ExternalID: {}", 
                        shopUrl, product.getSku(), externalProductId);
                return externalProductId;
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                log.error("Failed to publish product to Shopify - Shop: {}, SKU: {}, Status: {}", 
                        shopUrl, product.getSku(), response.code());
                throw new MarketplaceApiException("SHOPIFY", response.code(), 
                        "Failed to publish product: " + errorBody);
            }
        } catch (IOException e) {
            log.error("Error publishing product to Shopify - Shop: {}, SKU: {}", 
                    shopUrl, product.getSku(), e);
            throw new MarketplaceApiException("SHOPIFY", null, 
                    "Error publishing product: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void updateProduct(String externalProductId, Product product, Map<String, String> credentials) {
        String shopUrl = credentials.get("shop_url");
        String accessToken = credentials.get("access_token");
        
        String url = String.format("https://%s/admin/api/%s/products/%s.json", 
                shopUrl, API_VERSION, externalProductId);
        
        ObjectNode productNode = buildProductPayload(product);
        
        RequestBody body = RequestBody.create(productNode.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("X-Shopify-Access-Token", accessToken)
                .put(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("SHOPIFY", response.code(), 
                        "Failed to update product: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("SHOPIFY", null, 
                    "Error updating product: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void delistProduct(String externalProductId, Map<String, String> credentials) {
        String shopUrl = credentials.get("shop_url");
        String accessToken = credentials.get("access_token");
        
        String url = String.format("https://%s/admin/api/%s/products/%s.json", 
                shopUrl, API_VERSION, externalProductId);
        
        Request request = new Request.Builder()
                .url(url)
                .header("X-Shopify-Access-Token", accessToken)
                .delete()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("SHOPIFY", response.code(), 
                        "Failed to delist product: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("SHOPIFY", null, 
                    "Error delisting product: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fetch the full Shopify product catalogue, following Link-header pagination
     * (Shopify Admin REST switched from page=N to since_id/cursor-based; we use
     * the {@code Link: <…>; rel="next"} header). Each page caps at 250 products.
     * Returns the raw product JsonNode list so the caller can map fields against
     * the local Product / ProductVariant / image entities without losing
     * Shopify-specific bits (e.g. variant ids, image src).
     */
    public List<JsonNode> fetchProducts(Map<String, String> credentials) {
        String shopUrl = credentials.get("shop_url");
        String accessToken = credentials.get("access_token");
        if (shopUrl == null || accessToken == null) {
            throw new MarketplaceApiException("SHOPIFY", null, "Missing shop_url or access_token");
        }
        log.info("Fetching Shopify product catalogue from shop: {}", shopUrl);

        List<JsonNode> all = new ArrayList<>();
        String nextUrl = String.format("https://%s/admin/api/%s/products.json?limit=250", shopUrl, API_VERSION);
        int safetyLimit = 40; // cap at ~10k products to avoid runaway loops

        while (nextUrl != null && safetyLimit-- > 0) {
            Request request = new Request.Builder()
                    .url(nextUrl)
                    .header("X-Shopify-Access-Token", accessToken)
                    .get()
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    throw new MarketplaceApiException("SHOPIFY", response.code(),
                            "Failed to fetch Shopify products: " + errorBody);
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                JsonNode products = root.path("products");
                if (products.isArray()) {
                    products.forEach(all::add);
                }
                nextUrl = parseNextLink(response.header("Link"));
            } catch (IOException e) {
                throw new MarketplaceApiException("SHOPIFY", null,
                        "Error fetching Shopify products: " + e.getMessage(), e);
            }
        }

        log.info("Fetched {} Shopify products from shop: {}", all.size(), shopUrl);
        return all;
    }

    /** Extract the {@code <next-url>; rel="next"} entry from a Shopify Link header. */
    private String parseNextLink(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) return null;
        // Format: <https://…?page_info=abc&limit=250>; rel="next", <…>; rel="previous"
        for (String part : linkHeader.split(",")) {
            if (part.contains("rel=\"next\"")) {
                int lt = part.indexOf('<'), gt = part.indexOf('>');
                if (lt >= 0 && gt > lt) return part.substring(lt + 1, gt);
            }
        }
        return null;
    }

    @Override
    public List<ExternalOrder> fetchOrders(Map<String, String> credentials, LocalDateTime since) {
        String shopUrl = credentials.get("shop_url");
        String accessToken = credentials.get("access_token");
        if (shopUrl == null || accessToken == null) {
            throw new MarketplaceApiException("SHOPIFY", null, "Missing shop_url or access_token");
        }

        // Shopify's created_at_min expects ISO 8601 with timezone. Treat the
        // caller's LocalDateTime as UTC and emit "...Z" so the cutoff is
        // unambiguous regardless of shop locale.
        String createdAtMin = since.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        log.info("Fetching orders from Shopify - Shop: {}, Since: {} (UTC)", shopUrl, createdAtMin);

        HttpUrl firstUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(shopUrl)
                .addPathSegments("admin/api/" + API_VERSION + "/orders.json")
                .addQueryParameter("created_at_min", createdAtMin)
                .addQueryParameter("status", "any")
                .addQueryParameter("limit", "250")
                .build();

        List<ExternalOrder> all = new ArrayList<>();
        String nextUrl = firstUrl.toString();
        int safetyLimit = 40; // cap at ~10k orders per poll cycle

        while (nextUrl != null && safetyLimit-- > 0) {
            Request request = new Request.Builder()
                    .url(nextUrl)
                    .header("X-Shopify-Access-Token", accessToken)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                    log.error("Failed to fetch orders from Shopify - Shop: {}, Status: {}", shopUrl, response.code());
                    throw new MarketplaceApiException("SHOPIFY", response.code(),
                            "Failed to fetch orders: " + errorBody);
                }
                JsonNode jsonNode = objectMapper.readTree(response.body().string());
                List<ExternalOrder> page = parseOrders(jsonNode);
                all.addAll(page);
                nextUrl = parseNextLink(response.header("Link"));
            } catch (IOException e) {
                log.error("Error fetching orders from Shopify - Shop: {}", shopUrl, e);
                throw new MarketplaceApiException("SHOPIFY", null,
                        "Error fetching orders: " + e.getMessage(), e);
            }
        }

        log.info("Fetched {} orders from Shopify - Shop: {}", all.size(), shopUrl);
        return all;
    }
    
    @Override
    public void updateOrderStatus(String externalOrderId, String status, Map<String, String> credentials) {
        String shopUrl = credentials.get("shop_url");
        String accessToken = credentials.get("access_token");
        
        String url = String.format("https://%s/admin/api/%s/orders/%s/fulfillments.json", 
                shopUrl, API_VERSION, externalOrderId);
        
        ObjectNode fulfillmentNode = objectMapper.createObjectNode();
        ObjectNode fulfillment = objectMapper.createObjectNode();
        fulfillment.put("status", status);
        fulfillmentNode.set("fulfillment", fulfillment);
        
        RequestBody body = RequestBody.create(fulfillmentNode.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("X-Shopify-Access-Token", accessToken)
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new MarketplaceApiException("SHOPIFY", response.code(), 
                        "Failed to update order status: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("SHOPIFY", null, 
                    "Error updating order status: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void updateTracking(String externalOrderId, String trackingNumber, String courier, Map<String, String> credentials) {
        // First, we need to get the fulfillment ID for this order
        String shopUrl = credentials.get("shop_url");
        String accessToken = credentials.get("access_token");
        
        // Get fulfillments for the order
        String getFulfillmentsUrl = String.format("https://%s/admin/api/%s/orders/%s/fulfillments.json", 
                shopUrl, API_VERSION, externalOrderId);
        
        Request getRequest = new Request.Builder()
                .url(getFulfillmentsUrl)
                .header("X-Shopify-Access-Token", accessToken)
                .get()
                .build();
        
        try (Response getResponse = httpClient.newCall(getRequest).execute()) {
            if (getResponse.isSuccessful() && getResponse.body() != null) {
                JsonNode jsonNode = objectMapper.readTree(getResponse.body().string());
                JsonNode fulfillments = jsonNode.path("fulfillments");
                
                if (fulfillments.isArray() && fulfillments.size() > 0) {
                    String fulfillmentId = fulfillments.get(0).path("id").asText();
                    
                    // Update the fulfillment with tracking info
                    String updateUrl = String.format("https://%s/admin/api/%s/fulfillments/%s.json", 
                            shopUrl, API_VERSION, fulfillmentId);
                    
                    ObjectNode updateNode = objectMapper.createObjectNode();
                    ObjectNode fulfillment = objectMapper.createObjectNode();
                    fulfillment.put("tracking_number", trackingNumber);
                    fulfillment.put("tracking_company", courier);
                    updateNode.set("fulfillment", fulfillment);
                    
                    RequestBody body = RequestBody.create(updateNode.toString(), JSON);
                    Request updateRequest = new Request.Builder()
                            .url(updateUrl)
                            .header("X-Shopify-Access-Token", accessToken)
                            .put(body)
                            .build();
                    
                    try (Response updateResponse = httpClient.newCall(updateRequest).execute()) {
                        if (!updateResponse.isSuccessful()) {
                            String errorBody = updateResponse.body() != null ? updateResponse.body().string() : "No response body";
                            throw new MarketplaceApiException("SHOPIFY", updateResponse.code(), 
                                    "Failed to update tracking: " + errorBody);
                        }
                    }
                } else {
                    throw new MarketplaceApiException("SHOPIFY", 404, 
                            "No fulfillments found for order: " + externalOrderId);
                }
            } else {
                String errorBody = getResponse.body() != null ? getResponse.body().string() : "No response body";
                throw new MarketplaceApiException("SHOPIFY", getResponse.code(), 
                        "Failed to get fulfillments: " + errorBody);
            }
        } catch (IOException e) {
            throw new MarketplaceApiException("SHOPIFY", null, 
                    "Error updating tracking: " + e.getMessage(), e);
        }
    }
    
    /**
     * Builds Shopify product payload from Product entity.
     */
    private ObjectNode buildProductPayload(Product product) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode productNode = objectMapper.createObjectNode();
        
        productNode.put("title", product.getName());
        productNode.put("body_html", product.getDescription());
        productNode.put("vendor", product.getBrand() != null ? product.getBrand() : "");
        
        // Add variants
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            var variantsArray = objectMapper.createArrayNode();
            for (ProductVariant variant : product.getVariants()) {
                ObjectNode variantNode = objectMapper.createObjectNode();
                variantNode.put("sku", variant.getSku());
                variantNode.put("price", variant.getPrice().toString());
                variantNode.put("title", variant.getVariantName());
                variantsArray.add(variantNode);
            }
            productNode.set("variants", variantsArray);
        } else {
            // Single variant with base price
            var variantsArray = objectMapper.createArrayNode();
            ObjectNode variantNode = objectMapper.createObjectNode();
            variantNode.put("sku", product.getSku());
            variantNode.put("price", product.getBasePrice().toString());
            variantsArray.add(variantNode);
            productNode.set("variants", variantsArray);
        }
        
        // Add images. Shopify accepts either a public URL ("src") or inline
        // base64 ("attachment"). Product Studio uploads images as data: URLs so
        // the vision model can read them without public hosting — for those we
        // strip the data-URI prefix and send the raw base64 as "attachment".
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            var imagesArray = objectMapper.createArrayNode();
            for (String imageUrl : product.getImageUrls()) {
                if (imageUrl == null || imageUrl.isBlank()) continue;
                ObjectNode imageNode = objectMapper.createObjectNode();
                if (imageUrl.startsWith("data:")) {
                    int comma = imageUrl.indexOf(',');
                    if (comma < 0) continue; // malformed data URI — skip
                    imageNode.put("attachment", imageUrl.substring(comma + 1));
                } else {
                    imageNode.put("src", imageUrl);
                }
                imagesArray.add(imageNode);
            }
            if (!imagesArray.isEmpty()) {
                productNode.set("images", imagesArray);
            }
        }
        
        root.set("product", productNode);
        return root;
    }
    
    /**
     * Parses Shopify orders JSON response into ExternalOrder list.
     */
    private List<ExternalOrder> parseOrders(JsonNode jsonNode) {
        List<ExternalOrder> orders = new ArrayList<>();
        JsonNode ordersArray = jsonNode.path("orders");
        
        if (ordersArray.isArray()) {
            for (JsonNode orderNode : ordersArray) {
                String externalOrderId = orderNode.path("id").asText();
                BigDecimal totalAmount = new BigDecimal(orderNode.path("total_price").asText("0"));
                
                JsonNode customer = orderNode.path("customer");
                String customerName = customer.path("first_name").asText("") + " " + customer.path("last_name").asText("");
                String customerEmail = customer.path("email").asText("");
                String customerPhone = customer.path("phone").asText("");
                
                JsonNode shippingAddress = orderNode.path("shipping_address");
                String address = String.format("%s, %s, %s %s", 
                        shippingAddress.path("address1").asText(""),
                        shippingAddress.path("city").asText(""),
                        shippingAddress.path("province").asText(""),
                        shippingAddress.path("zip").asText(""));
                
                String createdAt = orderNode.path("created_at").asText();
                LocalDateTime orderedAt = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_DATE_TIME);
                
                // Parse line items
                List<ExternalOrderItem> items = new ArrayList<>();
                JsonNode lineItems = orderNode.path("line_items");
                if (lineItems.isArray()) {
                    for (JsonNode itemNode : lineItems) {
                        String productId = itemNode.path("product_id").asText();
                        String productName = itemNode.path("name").asText();
                        String variantName = itemNode.path("variant_title").asText("");
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

    // --- Autopilot Write Methods ---

    public boolean patchProduct(String externalProductId, JsonNode payload, Map<String, String> credentials) {
        String shopUrl = credentials.get("shop_url");
        String accessToken = credentials.get("access_token");
        
        String url = String.format("https://%s/admin/api/%s/products/%s.json", 
                shopUrl, API_VERSION, externalProductId);
        
        ObjectNode root = objectMapper.createObjectNode();
        root.set("product", payload);
        
        RequestBody body = RequestBody.create(root.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("X-Shopify-Access-Token", accessToken)
                .put(body) // Shopify uses PUT for partial updates on the product node
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to patch Shopify product {}: {}", externalProductId, response.body() != null ? response.body().string() : "No body");
                return false;
            }
            return true;
        } catch (IOException e) {
            log.error("Error patching Shopify product {}", externalProductId, e);
            return false;
        }
    }
    
    public boolean patchMetafield(String ownerId, String namespace, String key, String value, Map<String, String> credentials) {
        String shopUrl = credentials.get("shop_url");
        String accessToken = credentials.get("access_token");
        
        // For simplicity, assuming product metafields here
        String url = String.format("https://%s/admin/api/%s/products/%s/metafields.json", 
                shopUrl, API_VERSION, ownerId);
        
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode metafield = objectMapper.createObjectNode();
        metafield.put("namespace", namespace);
        metafield.put("key", key);
        metafield.put("value", value);
        metafield.put("type", "single_line_text_field");
        root.set("metafield", metafield);
        
        RequestBody body = RequestBody.create(root.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("X-Shopify-Access-Token", accessToken)
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to patch Shopify metafield for product {}: {}", ownerId, response.body() != null ? response.body().string() : "No body");
                return false;
            }
            return true;
        } catch (IOException e) {
            log.error("Error patching Shopify metafield for product {}", ownerId, e);
            return false;
        }
    }
}
