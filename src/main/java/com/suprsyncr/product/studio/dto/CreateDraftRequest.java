package com.suprsyncr.product.studio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body for POST /api/v1/products/studio/draft.
 *
 * The seller supplies the product image (as an http(s) URL or a base64
 * {@code data:} URL — the engine passes it straight to the vision model, so
 * either works without needing the image to be publicly hosted) and the title
 * they typed in — possibly wrong, which is the whole point of the mismatch
 * detection in the Studio manager.
 *
 * {@link Services} controls which specialist columns the engine generates so
 * we never spend model calls on services the seller didn't ask for.
 */
@Data
public class CreateDraftRequest {

    @NotBlank
    private String storeId;

    @NotBlank
    private String imageUrl;

    @NotBlank
    @Size(max = 512)
    private String claimedTitle;

    private String posture = "balanced";

    private Services services = new Services();

    /** The three studio services. Defaults to all enabled. */
    @Data
    public static class Services {
        /** Product optimisation → listing copy (title, bullets, description). */
        private boolean product = true;
        /** SEO optimisation → handle, tags, search terms, meta. */
        private boolean seo = true;
        /** Platform metadata → product type, vendor, attributes. */
        private boolean platform = true;
    }
}
