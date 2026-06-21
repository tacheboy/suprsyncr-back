package com.suprsyncr.product.studio.internal;

import com.suprsyncr.seller.entity.SellerPlatform;
import com.suprsyncr.seller.repository.SellerPlatformRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the {@code storeId} string passed by the engine to a backing
 * {@code sellerId}. Mirrors the convention used by
 * {@code ShopifyCredentialResolver}: numeric storeIds are {@link SellerPlatform}
 * primary keys; non-numeric ids (e.g. {@code "store-a"}) are demo stores that
 * have no real seller behind them.
 */
@Component
@RequiredArgsConstructor
public class StoreContextResolver {

    private final SellerPlatformRepository platformRepository;

    /** Returns the seller id for a real store, or empty for demo/unknown ids. */
    public Optional<Long> resolveSellerId(String storeId) {
        Long platformId = parsePlatformId(storeId);
        if (platformId == null) return Optional.empty();
        return platformRepository.findById(platformId)
                .map(p -> p.getSeller().getId());
    }

    private Long parsePlatformId(String storeId) {
        if (storeId == null || storeId.isBlank()) return null;
        try {
            return Long.parseLong(storeId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
