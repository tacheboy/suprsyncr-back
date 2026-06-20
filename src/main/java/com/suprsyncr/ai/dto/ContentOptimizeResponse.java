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
public class ContentOptimizeResponse {
    private String optimisedTitle;
    private List<String> bulletPoints;
    private String productDescription;
    private List<String> searchKeywords;
    private String improvementNotes;
    private String crossPlatformImpact;
    private int seoScoreBefore;
    private int seoScoreAfter;
    private long latencyMs;
}

