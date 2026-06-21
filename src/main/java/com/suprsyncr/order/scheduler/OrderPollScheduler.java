package com.suprsyncr.order.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.integration.connector.ConnectorRegistry;
import com.suprsyncr.integration.connector.ExternalOrder;
import com.suprsyncr.integration.connector.MarketplaceConnector;
import com.suprsyncr.integration.entity.ConnectorFailure;
import com.suprsyncr.integration.entity.FailureType;
import com.suprsyncr.integration.repository.ConnectorFailureRepository;
import com.suprsyncr.notification.entity.NotificationType;
import com.suprsyncr.notification.service.StoreNotificationService;
import com.suprsyncr.order.repository.OrderRepository;
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
    private final OrderRepository orderRepository;
    private final CredentialEncryptionService credentialEncryptionService;
    private final ConnectorFailureRepository connectorFailureRepository;
    private final StoreNotificationService notificationService;
    private final ObjectMapper objectMapper;

    public OrderPollScheduler(
        SellerPlatformRepository sellerPlatformRepository,
        ConnectorRegistry connectorRegistry,
        OrderService orderService,
        OrderRepository orderRepository,
        CredentialEncryptionService credentialEncryptionService,
        ConnectorFailureRepository connectorFailureRepository,
        StoreNotificationService notificationService,
        ObjectMapper objectMapper
    ) {
        this.sellerPlatformRepository = sellerPlatformRepository;
        this.connectorRegistry = connectorRegistry;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.credentialEncryptionService = credentialEncryptionService;
        this.connectorFailureRepository = connectorFailureRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Polls all connected platforms for new orders every 2 minutes.
     * Uses fixed delay to prevent overlapping executions.
     * initialDelay = 5s so a newly-restarted backend pulls orders quickly
     * instead of making the operator wait a full poll cycle.
     */
    @Scheduled(fixedDelay = 120000, initialDelay = 5000)
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
            
            // Determine since timestamp: lastSyncedAt or last 90 days on first poll.
            // 90 days gives newly-connected stores a meaningful historical backlog
            // for evidence/funnel signals. After the first successful poll
            // lastSyncedAt is set to now() so subsequent polls only pull deltas.
            LocalDateTime since = platform.getLastSyncedAt() != null
                ? platform.getLastSyncedAt()
                : LocalDateTime.now().minusDays(90);
            
            logger.debug("Fetching orders for platform {} since {}", platform.getId(), since);
            
            // Fetch orders from marketplace
            List<ExternalOrder> externalOrders = connector.fetchOrders(credentials, since);
            
            logger.info("Fetched {} orders from platform {}", externalOrders.size(), platform.getId());
            
            // Ingest each order; fire a notification only for genuinely new orders
            for (ExternalOrder externalOrder : externalOrders) {
                try {
                    boolean isNew = orderRepository
                            .findByExternalOrderId(externalOrder.externalOrderId()).isEmpty();
                    orderService.ingestOrder(platform.getId(), externalOrder);
                    if (isNew) {
                        try {
                            notificationService.createInternalNotification(
                                    platform, NotificationType.NEW_ORDER, "orders/polled",
                                    externalOrder.externalOrderId(),
                                    externalOrder.customerName(),
                                    externalOrder.totalAmount(), "INR");
                        } catch (Exception ne) {
                            logger.warn("notification skipped for order {}: {}",
                                    externalOrder.externalOrderId(), ne.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to ingest order {} from platform {}: {}",
                        externalOrder.externalOrderId(), platform.getId(), e.getMessage());
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
    
    private Map<String, String> parseCredentials(String credentialsJson) {
        try {
            Map<String, String> credentials = objectMapper.readValue(
                    credentialsJson, new TypeReference<Map<String, String>>() {});
            if (credentials == null) {
                throw new IllegalArgumentException("Decrypted platform credentials JSON must not be null");
            }
            return credentials;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse decrypted platform credentials", e);
        }
    }
}

