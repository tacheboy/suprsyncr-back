package com.suprsyncr.ai.prompt;

import com.suprsyncr.ai.dto.ProductGenerateRequest;

public class ProductGeneratorPrompt {

    public static String system() {
        return "You are an expert Indian e-commerce catalog manager. " +
               "Your task is to generate complete, high-converting product listings from scratch based on a short seller intent and a product image. " +
               "Provide a realistic MRP suggestion in INR. " +
               "IMPORTANT: mrpSuggestionInr MUST be a number (e.g. 1500.0), NOT a string. " +
               "You MUST respond ONLY with a valid JSON object matching this structure EXACTLY. " +
               "Do not include any markdown wrappers, code fences, or commentary outside the JSON.\n" +
               "{\n" +
               "  \"suggestedTitle\": \"string\",\n" +
               "  \"brand\": \"string\",\n" +
               "  \"category\": \"string\",\n" +
               "  \"mrpSuggestionInr\": 1500.0,\n" +
               "  \"bulletPoints\": [\"string\", \"string\"],\n" +
               "  \"productDescription\": \"string\",\n" +
               "  \"keyFeatures\": [\"string\", \"string\"],\n" +
               "  \"suggestedAttributes\": {\"Material\": \"Cotton\"},\n" +
               "  \"amazonKeywords\": [\"string\", \"string\"],\n" +
               "  \"confidenceNote\": \"string\"\n" +
               "}";
    }

    public static String user(ProductGenerateRequest request) {
        return String.format(
            "Please generate a product listing based on the provided image and the following seller intent:\n" +
            "Seller Intent: %s\n" +
            "Preferred Category: %s\n",
            request.getSellerIntent(), request.getPreferredCategory() != null ? request.getPreferredCategory() : "Auto-detect"
        );
    }
}

