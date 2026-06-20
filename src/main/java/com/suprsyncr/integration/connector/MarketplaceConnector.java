package com.suprsyncr.integration.connector;

import com.suprsyncr.product.entity.Product;
import com.suprsyncr.seller.dto.MarketplaceOnboardingData;
import com.suprsyncr.seller.entity.PlatformType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Interface for marketplace platform connectors.
 * Provides methods for testing connections, creating seller accounts, and managing products and orders.
 */
public interface MarketplaceConnector {
    
    /**
     * Gets the platform type this connector handles.
     * 
     * @return the platform type
     */
    PlatformType getPlatformType();
    
    /**
     * Tests the connection to the marketplace platform.
     * 
     * @param credentials the platform credentials
     * @return ConnectionTestResult with success status and message
     */
    ConnectionTestResult testConnection(Map<String, String> credentials);
    
    /**
     * Creates a new seller account on the marketplace platform.
     * 
     * @param onboardingData the seller onboarding data
     * @return map of credentials for the created account
     * @throws UnsupportedOperationException if the platform doesn't support automatic account creation
     */
    Map<String, String> createSellerAccount(MarketplaceOnboardingData onboardingData);
    
    /**
     * Publishes a product to the marketplace platform.
     * 
     * @param product the product to publish
     * @param credentials the platform credentials
     * @return the external product ID assigned by the marketplace
     */
    String publishProduct(Product product, Map<String, String> credentials);
    
    /**
     * Updates an existing product on the marketplace platform.
     * 
     * @param externalProductId the external product ID on the marketplace
     * @param product the updated product data
     * @param credentials the platform credentials
     */
    void updateProduct(String externalProductId, Product product, Map<String, String> credentials);
    
    /**
     * Delists (removes) a product from the marketplace platform.
     * 
     * @param externalProductId the external product ID on the marketplace
     * @param credentials the platform credentials
     */
    void delistProduct(String externalProductId, Map<String, String> credentials);
    
    /**
     * Fetches orders from the marketplace platform.
     * 
     * @param credentials the platform credentials
     * @param since fetch orders created since this timestamp
     * @return list of external orders
     */
    List<ExternalOrder> fetchOrders(Map<String, String> credentials, LocalDateTime since);
    
    /**
     * Updates the order status on the marketplace platform.
     * 
     * @param externalOrderId the external order ID on the marketplace
     * @param status the new order status
     * @param credentials the platform credentials
     */
    void updateOrderStatus(String externalOrderId, String status, Map<String, String> credentials);
    
    /**
     * Updates tracking information for an order on the marketplace platform.
     * 
     * @param externalOrderId the external order ID on the marketplace
     * @param trackingNumber the tracking number
     * @param courier the courier/shipping partner name
     * @param credentials the platform credentials
     */
    void updateTracking(String externalOrderId, String trackingNumber, String courier, Map<String, String> credentials);
}
