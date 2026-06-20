package com.suprsyncr.ai.prompt;

import com.suprsyncr.ai.dto.ContentOptimizeRequest;

public class AmazonOptimizePrompt {
    
    public static String system() {
        return "You are an expert e-commerce copywriter specializing in Indian marketplaces like Amazon India, Flipkart, Myntra, etc. " +
               "Your task is to optimize product listings for maximum SEO visibility and conversion. " +
               "You MUST explicitly tell the user what content for the listing is the most impactful across these different platforms in the 'crossPlatformImpact' field. " +
               "You will be provided with a product image and the current listing details. " +
               "IMPORTANT: seoScoreBefore and seoScoreAfter MUST be integers (e.g. 45), NOT strings. " +
               "You MUST respond ONLY with a valid JSON object matching this structure EXACTLY. " +
               "Do not include any markdown wrappers, code fences, or commentary outside the JSON.\n" +
               "{\n" +
               "  \"optimisedTitle\": \"string\",\n" +
               "  \"bulletPoints\": [\"string\", \"string\"],\n" +
               "  \"productDescription\": \"string\",\n" +
               "  \"searchKeywords\": [\"string\", \"string\"],\n" +
               "  \"improvementNotes\": \"string\",\n" +
               "  \"crossPlatformImpact\": \"string\",\n" +
               "  \"seoScoreBefore\": 45,\n" +
               "  \"seoScoreAfter\": 95\n" +
               "}";
    }

    public static String user(ContentOptimizeRequest request) {
        return String.format(
            "Please optimize this product listing based on the provided image and the following details:\n" +
            "Current Title: %s\n" +
            "Current Description: %s\n" +
            "Category: %s\n",
            request.getCurrentTitle(), request.getCurrentDescription(), request.getCategory()
        );
    }
}

