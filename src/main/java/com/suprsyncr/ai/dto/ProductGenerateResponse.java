package com.suprsyncr.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductGenerateResponse {
    private String suggestedTitle;
    private String brand;
    private String category;
    private double mrpSuggestionInr;
    private List<String> bulletPoints;
    private String productDescription;
    private List<String> keyFeatures;
    private Map<String, String> suggestedAttributes;
    private List<String> amazonKeywords;
    private String confidenceNote;
    private long latencyMs;
}

