package com.suprsyncr.integration.shopify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.seller.entity.ConnectionStatus;
import com.suprsyncr.seller.entity.PlatformType;
import com.suprsyncr.seller.entity.SellerPlatform;
import com.suprsyncr.seller.repository.SellerPlatformRepository;
import com.suprsyncr.seller.service.CredentialEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Resolves a logical analytics/autopilot {@code storeId} to the seller's stored,
 * decrypted Shopify credentials ({@code shop_url}, {@code access_token}).
 *
 * <p>The autopilot and analytics layers identify a store by a {@code String storeId}.
 * For a real connected store this is the {@link SellerPlatform} primary key (see the
 * frontend {@code useActiveStoreId} hook, which uses {@code platform.id}). The demo
 * store uses the literal id {@code "store-a"}, which is not a Shopify store.</p>
 *
 * <p>This is the single source of truth for "is this store a connected Shopify store,
 * and what are its credentials" — used by both the action layer (apply/rollback) and
 * the perception layer ({@code ShopifyStoreDataProvider}). Credentials are never sent
 * by the client.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopifyCredentialResolver {

    private final SellerPlatformRepository platformRepository;
    private final CredentialEncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    /**
     * Resolve the connected Shopify {@link SellerPlatform} for a storeId, if one exists.
     * Returns empty when the storeId is not a numeric platform id, the platform is not
     * Shopify, or it is not in CONNECTED status.
     */
    public Optional<SellerPlatform> findConnectedShopifyPlatform(String storeId) {
        Long platformId = parsePlatformId(storeId);
        if (platformId == null) {
            return Optional.empty();
        }
        return platformRepository.findById(platformId)
                .filter(p -> p.getPlatformType() == PlatformType.SHOPIFY)
                .filter(p -> p.getStatus() == ConnectionStatus.CONNECTED);
    }

    /**
     * @return true when the storeId maps to a connected Shopify store.
     */
    public boolean isShopifyConnected(String storeId) {
        return findConnectedShopifyPlatform(storeId).isPresent();
    }

    /**
     * Load and decrypt the Shopify credentials for a storeId.
     *
     * @throws IllegalStateException when the store is not a connected Shopify store,
     *                               or its credentials cannot be decrypted.
     */
    public Map<String, String> resolveCredentials(String storeId) {
        SellerPlatform platform = findConnectedShopifyPlatform(storeId)
                .orElseThrow(() -> new IllegalStateException(
                        "No connected Shopify store for storeId: " + storeId));
        return decryptCredentials(platform);
    }

    /**
     * Decrypt the stored credential JSON of a platform into a {shop_url, access_token} map.
     */
    public Map<String, String> decryptCredentials(SellerPlatform platform) {
        try {
            String json = encryptionService.decrypt(platform.getEncryptedCredentials());
            @SuppressWarnings("unchecked")
            Map<String, String> creds = objectMapper.readValue(json, Map.class);
            if (creds.get("shop_url") == null || creds.get("access_token") == null) {
                throw new IllegalStateException(
                        "Stored Shopify credentials missing shop_url/access_token for platform " + platform.getId());
            }
            return creds;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to decrypt Shopify credentials for platform " + platform.getId(), e);
        }
    }

    private Long parsePlatformId(String storeId) {
        if (storeId == null || storeId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(storeId.trim());
        } catch (NumberFormatException e) {
            // Demo/dummy stores like "store-a" are not numeric — not a real platform.
            return null;
        }
    }
}
