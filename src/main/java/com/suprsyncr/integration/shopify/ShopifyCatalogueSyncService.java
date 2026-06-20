package com.suprsyncr.integration.shopify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.integration.connector.ShopifyConnector;
import com.suprsyncr.product.entity.Product;
import com.suprsyncr.product.entity.ProductStatus;
import com.suprsyncr.product.entity.ProductVariant;
import com.suprsyncr.product.repository.ProductRepository;
import com.suprsyncr.product.repository.ProductVariantRepository;
import com.suprsyncr.seller.entity.ConnectionStatus;
import com.suprsyncr.seller.entity.PlatformType;
import com.suprsyncr.seller.entity.SellerPlatform;
import com.suprsyncr.seller.repository.SellerPlatformRepository;
import com.suprsyncr.seller.service.CredentialEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Imports the connected Shopify storefront's catalogue (products, variants,
 * images) into the local {@code products}/{@code product_variants} tables so
 * the rest of the system — Products UI, analytics evidence, autopilot apply —
 * can operate on the seller's real data instead of the demo catalogue.
 *
 * <p>The sync is idempotent: products are upserted by their generated SKU
 * ({@code shopify-<productId>}) so re-running the sync updates existing rows
 * rather than duplicating them. Variant rows are upserted by their Shopify
 * variant id encoded into the variant SKU.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopifyCatalogueSyncService {

    private final SellerPlatformRepository platformRepository;
    private final CredentialEncryptionService encryptionService;
    private final ShopifyConnector shopifyConnector;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ObjectMapper objectMapper;

    public record SyncResult(int productsUpserted, int variantsUpserted, int productsFetched, String error) {
        public boolean ok() { return error == null; }
    }

    /**
     * Pull every product from the seller's connected Shopify storefront and
     * upsert it into the local catalogue. Returns counts; never throws — a
     * failure is reported via {@link SyncResult#error}. Wrapped in a single
     * transaction so a partial Shopify failure doesn't leave half-imported rows.
     */
    @Transactional
    public SyncResult syncCatalogue(Long platformId) {
        SellerPlatform platform = platformRepository.findById(platformId).orElse(null);
        if (platform == null) {
            return new SyncResult(0, 0, 0, "Platform not found: " + platformId);
        }
        if (platform.getPlatformType() != PlatformType.SHOPIFY) {
            return new SyncResult(0, 0, 0, "Platform is not Shopify: " + platform.getPlatformType());
        }
        if (platform.getStatus() != ConnectionStatus.CONNECTED) {
            return new SyncResult(0, 0, 0, "Platform is not connected: " + platform.getStatus());
        }

        Map<String, String> credentials;
        try {
            String decrypted = encryptionService.decrypt(platform.getEncryptedCredentials());
            @SuppressWarnings("unchecked")
            Map<String, String> creds = objectMapper.readValue(decrypted, Map.class);
            credentials = creds;
        } catch (Exception e) {
            return new SyncResult(0, 0, 0, "Failed to decrypt credentials: " + e.getMessage());
        }

        List<JsonNode> shopifyProducts;
        try {
            shopifyProducts = shopifyConnector.fetchProducts(credentials);
        } catch (Exception e) {
            log.error("Shopify product fetch failed for platform {}: {}", platformId, e.getMessage());
            platform.setLastSyncError("Fetch failed: " + e.getMessage());
            platformRepository.save(platform);
            return new SyncResult(0, 0, 0, "Shopify fetch failed: " + e.getMessage());
        }

        int productsUpserted = 0;
        int variantsUpserted = 0;
        Long sellerId = platform.getSeller().getId();

        for (JsonNode sp : shopifyProducts) {
            String externalId = sp.path("id").asText();
            if (externalId.isBlank()) continue;
            String sku = "shopify-" + externalId;
            try {
                Product product = productRepository.findBySellerIdAndSku(sellerId, sku).orElseGet(Product::new);
                if (product.getSeller() == null) product.setSeller(platform.getSeller());
                product.setSku(sku);
                product.setName(textOrFallback(sp.path("title"), "Untitled product"));
                product.setDescription(stripHtml(sp.path("body_html").asText("")));
                product.setBrand(sp.path("vendor").asText(null));
                product.setStatus(mapStatus(sp.path("status").asText("active")));
                product.setBasePrice(firstVariantPrice(sp));
                product.setImageUrls(collectImageUrls(sp));
                product = productRepository.save(product);
                productsUpserted++;

                variantsUpserted += upsertVariants(product, sp);
            } catch (Exception e) {
                log.warn("Skipping Shopify product {} due to error: {}", externalId, e.getMessage());
            }
        }

        platform.setLastSyncedAt(LocalDateTime.now());
        platform.setLastSyncError(null);
        platformRepository.save(platform);

        log.info("Shopify catalogue sync for platform {} complete: fetched={}, products={}, variants={}",
                platformId, shopifyProducts.size(), productsUpserted, variantsUpserted);
        return new SyncResult(productsUpserted, variantsUpserted, shopifyProducts.size(), null);
    }

    // --- helpers ---------------------------------------------------------------

    private int upsertVariants(Product product, JsonNode shopifyProduct) {
        JsonNode variants = shopifyProduct.path("variants");
        if (!variants.isArray() || variants.isEmpty()) return 0;
        int count = 0;
        for (JsonNode v : variants) {
            String variantExternalId = v.path("id").asText();
            if (variantExternalId.isBlank()) continue;
            String variantSku = v.path("sku").asText("");
            if (variantSku.isBlank()) variantSku = "shopify-variant-" + variantExternalId;

            ProductVariant variant = variantRepository
                    .findByProductIdAndSku(product.getId(), variantSku)
                    .orElseGet(ProductVariant::new);
            variant.setProduct(product);
            variant.setSku(variantSku);
            variant.setVariantName(textOrFallback(v.path("title"), "Default"));
            variant.setPrice(decimalOrZero(v.path("price").asText("0")));
            String imageId = v.path("image_id").asText(null);
            variant.setImageUrl(findImageById(shopifyProduct, imageId));

            // Persist option1/option2/option3 + Shopify id as JSON attributes (JSONB column)
            Map<String, String> attrs = new HashMap<>();
            if (v.hasNonNull("option1")) attrs.put("option1", v.path("option1").asText());
            if (v.hasNonNull("option2")) attrs.put("option2", v.path("option2").asText());
            if (v.hasNonNull("option3")) attrs.put("option3", v.path("option3").asText());
            attrs.put("shopify_variant_id", variantExternalId);
            try {
                variant.setAttributes(objectMapper.writeValueAsString(attrs));
            } catch (Exception ignored) {
                variant.setAttributes("{}");
            }

            variantRepository.save(variant);
            count++;
        }
        return count;
    }

    private static List<String> collectImageUrls(JsonNode shopifyProduct) {
        List<String> urls = new ArrayList<>();
        JsonNode images = shopifyProduct.path("images");
        if (images.isArray()) {
            for (JsonNode img : images) {
                String src = img.path("src").asText(null);
                if (src != null && !src.isBlank()) urls.add(src);
            }
        }
        return urls;
    }

    private static String findImageById(JsonNode shopifyProduct, String imageId) {
        if (imageId == null || imageId.isBlank()) return null;
        JsonNode images = shopifyProduct.path("images");
        if (!images.isArray()) return null;
        for (JsonNode img : images) {
            if (imageId.equals(img.path("id").asText())) {
                return img.path("src").asText(null);
            }
        }
        return null;
    }

    private static BigDecimal firstVariantPrice(JsonNode shopifyProduct) {
        JsonNode variants = shopifyProduct.path("variants");
        if (variants.isArray() && !variants.isEmpty()) {
            return decimalOrZero(variants.get(0).path("price").asText("0"));
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal decimalOrZero(String s) {
        try { return new BigDecimal(s); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private static ProductStatus mapStatus(String shopifyStatus) {
        // Shopify: "active" | "archived" | "draft"
        return switch (shopifyStatus == null ? "" : shopifyStatus.toLowerCase()) {
            case "archived" -> ProductStatus.ARCHIVED;
            case "draft" -> ProductStatus.DRAFT;
            default -> ProductStatus.ACTIVE;
        };
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private static String textOrFallback(JsonNode node, String fallback) {
        String s = node.asText("");
        return s.isBlank() ? fallback : s;
    }
}
