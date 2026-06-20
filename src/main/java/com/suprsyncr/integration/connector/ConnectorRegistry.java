package com.suprsyncr.integration.connector;

import com.suprsyncr.common.exception.UnsupportedPlatformException;
import com.suprsyncr.seller.entity.PlatformType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for marketplace connectors and webhook validators.
 * Autowires all MarketplaceConnector and WebhookValidator beans and provides lookup by platform type.
 * 
 * Validates: Requirements 72, 73
 */
@Component
public class ConnectorRegistry {
    
    private final Map<PlatformType, MarketplaceConnector> connectors = new HashMap<>();
    private final Map<PlatformType, WebhookValidator> validators = new HashMap<>();
    
    private final List<MarketplaceConnector> connectorList;
    private final List<WebhookValidator> validatorList;
    
    @Autowired(required = false)
    public ConnectorRegistry(List<MarketplaceConnector> connectorList, List<WebhookValidator> validatorList) {
        this.connectorList = connectorList;
        this.validatorList = validatorList;
    }
    
    /**
     * Initializes the registry by building maps of connectors and validators by platform type.
     */
    @PostConstruct
    public void init() {
        // Build connector map
        if (connectorList != null) {
            for (MarketplaceConnector connector : connectorList) {
                connectors.put(connector.getPlatformType(), connector);
            }
        }
        
        // Build validator map
        if (validatorList != null) {
            for (WebhookValidator validator : validatorList) {
                validators.put(validator.getPlatformType(), validator);
            }
        }
    }
    
    /**
     * Gets a connector for the specified platform type.
     * 
     * @param platformType the platform type
     * @return the marketplace connector
     * @throws UnsupportedPlatformException if no connector exists for the platform
     */
    public MarketplaceConnector getConnector(PlatformType platformType) {
        MarketplaceConnector connector = connectors.get(platformType);
        if (connector == null) {
            throw new UnsupportedPlatformException(platformType, "No connector available for platform: " + platformType);
        }
        return connector;
    }
    
    /**
     * Gets a webhook validator for the specified platform type.
     * 
     * @param platformType the platform type
     * @return the webhook validator
     * @throws UnsupportedPlatformException if no validator exists for the platform
     */
    public WebhookValidator getValidator(PlatformType platformType) {
        WebhookValidator validator = validators.get(platformType);
        if (validator == null) {
            throw new UnsupportedPlatformException(platformType, "No validator available for platform: " + platformType);
        }
        return validator;
    }
}
