package com.suprsyncr.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformSuggestResponse {
    private String productName;
    private String productCategory;
    private List<PlatformRecommendation> recommendations;
    private String overallStrategy;
    private String pricingAdvice;
    private long latencyMs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlatformRecommendation {
        private String platform;
        private int fitScore;
        private String expectedReach;
        private String expectedMargin;
        private String rationale;
        private List<String> pros;
        private List<String> cons;
        private String priority; // HIGH | MEDIUM | LOW
    }
}

