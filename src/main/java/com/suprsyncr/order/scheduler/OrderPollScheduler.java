package com.suprsyncr.order.scheduler;

import com.suprsyncr.integration.connector.ConnectorRegistry;
import com.suprsyncr.integration.connector.ExternalOrder;
import com.suprsyncr.integration.connector.MarketplaceConnector;
import com.suprsyncr.integration.entity.ConnectorFailure;
import com.suprsyncr.integration.entity.FailureType;
import com.suprsyncr.integration.repository.ConnectorFailureRepository;
import com.suprsyncr.order.service.OrderService;
import com.suprsyncr.seller.entity.ConnectionStatus;
import com.suprsyncr.seller.entity.SellerPlatform;
import com.suprsyncr.seller.repository.SellerPlatformRepository;
import com.suprsyncr.seller.service.CredentialEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduler component that polls marketplace platforms for new orders.
 * Runs every 2 minutes to fetch orders from connected platforms.
 * 
 * Requirements: 22, 85
 */
@Component
@EnableScheduling
public class OrderPollScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderPollScheduler.class);
    
    private final SellerPlatformRepository sellerPlatformRepository;
    private final ConnectorRegistry connectorRegistry;
    private final OrderService orderService;
    private final CredentialEncryptionService credentialEncryptionService;
    private final ConnectorFailureRepository connectorFailureRepository;
    
    public OrderPollScheduler(
        SellerPlatformRepository sellerPlatformRepository,
        ConnectorRegistry connectorRegistry,
        OrderService orderService,
        CredentialEncryptionService credentialEncryptionService,
        ConnectorFailureRepository connectorFailureRepository
    ) {
        this.sellerPlatformRepository = sellerPlatformRepository;
        this.connectorRegistry = connectorRegistry;
        this.orderService = orderService;
        this.credentialEncryptionService = credentialEncryptionService;
        this.connectorFailureRepository = connectorFailureRepository;
    }
    
    /**
     * Polls all connected platforms for new orders every 2 minutes.
     * Uses fixed delay to prevent overlapping executions.
     */
    @Scheduled(fixedDelay = 120000) // 2 minutes = 120000 milliseconds
    public void pollOrders() {
        logger.info("Starting scheduled order polling");
        
        // Fetch all platforms with CONNECTED status
        List<SellerPlatform> connectedPlatforms = sellerPlatformRepository.findByStatus(ConnectionStatus.CONNECTED);
        
        logger.info("Found {} connected platforms to poll", connectedPlatforms.size());
        
        // Poll each platform
        for (SellerPlatform platform : connectedPlatforms) {
            try {
                pollPlatform(platform);
            } catch (Exception e) {
                // Log error and create ConnectorFailure, but continue with other platforms
                logger.error("Failed to poll platform {}: {}", platform.getId(), e.getMessage(), e);
                createConnectorFailure(platform, e);
            }
        }
        
        logger.info("Completed scheduled order polling");
    }
    
    /**
     * Polls a single platform for orders.
     */
    private void pollPlatform(SellerPlatform platform) {
        logger.info("Polling platform {} ({})", platform.getId(), platform.getPlatformType());
        
        try {
            // Get connector for platform type
            MarketplaceConnector connector = connectorRegistry.getConnector(platform.getPlatformType());
            
            // Decrypt credentials
            String decryptedCredentials = credentialEncryptionService.decrypt(platform.getEncryptedCredentials());
            Map<String, String> credentials = parseCredentials(decryptedCredentials);
            
            // Determine since timestamp: lastSyncedAt or last 7 days if null
            LocalDateTime since = platform.getLastSyncedAt() != null 
                ? platform.getLastSyncedAt() 
                : LocalDateTime.now().minusDays(7);
            
            logger.debug("Fetching orders for platform {} since {}", platform.getId(), since);
            
            // Fetch orders from marketplace
            List<ExternalOrder> externalOrders = connector.fetchOrders(credentials, since);
            
            logger.info("Fetched {} orders from platform {}", externalOrders.size(), platform.getId());
            
            // Ingest each order
            for (ExternalOrder externalOrder : externalOrders) {
                try {
                    orderService.ingestOrder(platform.getId(), externalOrder);
                } catch (Exception e) {
                    logger.error("Failed to ingest order {} from platform {}: {}", 
                        externalOrder.externalOrderId(), platform.getId(), e.getMessage());
                    // Continue with next order
                }
            }
            
            // Update lastSyncedAt on success
            platform.setLastSyncedAt(LocalDateTime.now());
            platform.setLastSyncError(null);
            sellerPlatformRepository.save(platform);
            
            logger.info("Successfully polled platform {}", platform.getId());
            
        } catch (Exception e) {
            // Log error and update platform with error message
            logger.error("Error polling platform {}: {}", platform.getId(), e.getMessage(), e);
            platform.setLastSyncError(e.getMessage());
            sellerPlatformRepository.save(platform);
            
            // Create connector failure record
            createConnectorFailure(platform, e);
            
            // Don't rethrow - continue with other platforms
        }
    }
    
    /**
     * Creates a connector failure record for tracking.
     */
    private void createConnectorFailure(SellerPlatform platform, Exception e) {
        try {
            ConnectorFailure failure = new ConnectorFailure();
            failure.setPlatform(platform);
            failure.setFailureType(determineFailureType(e));
            failure.setErrorMessage(e.getMessage());
            failure.setStackTrace(getStackTraceAsString(e));
            connectorFailureRepository.save(failure);
        } catch (Exception ex) {
            logger.error("Failed to create connector failure record: {}", ex.getMessage());
        }
    }
    
    /**
     * Determines the failure type based on exception.
     */
    private FailureType determineFailureType(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        if (message.contains("timeout") || message.contains("timed out")) {
            return FailureType.TIMEOUT;
        } else if (message.contains("auth") || message.contains("unauthorized") || message.contains("forbidden")) {
            return FailureType.AUTHENTICATION;
        } else if (message.contains("connection") || message.contains("connect")) {
            return FailureType.CONNECTION;
        } else {
            return FailureType.API_ERROR;
        }
    }
    
    /**
     * Converts exception stack trace to string.
     */
    private String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Parses JSON credentials string to map.
     * This is a simplified implementation - in production use Jackson or Gson.
     */
    private Map<String, String> parseCredentials(String credentialsJson) {
        // Placeholder implementation
        // In production, this would use proper JSON parsing
        Map<String, String> credentials = new HashMap<>();
        // The actual implementation would parse the JSON string
        return credentials;
    }
}

