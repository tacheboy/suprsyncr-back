package com.suprsyncr.analytics.service;

import com.suprsyncr.ai.client.OpenAiClient;
import com.suprsyncr.analytics.dto.ProductHealthResponse;
import com.suprsyncr.analytics.dto.RevenueLeakResponse;
import com.suprsyncr.analytics.dto.SeoGapResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiInsightService {

    private final OpenAiClient openAiClient;

    private static final String SYSTEM_PROMPT = """
            You are a sharp, data-driven e-commerce analyst advising Indian D2C brands.
            Your role is to write brief, specific, actionable analyst notes.
            Rules:
            - Always reference actual numbers from the data.
            - 2 sentences maximum.
            - First sentence: identify the specific cause of the metric.
            - Second sentence: one concrete action they can take today.
            - Never use hedging language: no "it seems", "you might want to", "consider possibly".
            - Be direct. Write like a consultant who is being paid by the hour.
            - Always think in rupees (INR) — this is an Indian market.
            """;

    public String generateRevenuLeakInsight(RevenueLeakResponse leak) {
        if (leak.getByProduct() == null || leak.getByProduct().isEmpty()) {
            return null;
        }

        RevenueLeakResponse.ProductLeakBreakdown worst = leak.getByProduct().get(0);
        RevenueLeakResponse.SourceLeakBreakdown topSource = leak.getBySource().isEmpty()
                ? null : leak.getBySource().get(0);
        String topSourceName = topSource != null ? topSource.getSource() : "your primary channel";
        double topSourceAbandonment = topSource != null ? topSource.getAbandonmentRate() : 0.0;

        String prompt = """
                Store: %s
                Last 30 days data:
                - Total revenue lost to cart abandonment + checkout drop: ₹%s
                - Cart abandonment loss: ₹%s (%.0f%% abandonment rate)
                - Checkout drop loss: ₹%s
                - Worst product: "%s" — %.0f%% abandonment, losing ₹%s/month
                - Primary traffic source: %s (%.0f%% abandonment rate on this channel)

                Write exactly 2 sentences as instructed.
                """.formatted(
                leak.getStoreName(),
                formatINR(leak.getTotalLeakINR()),
                formatINR(leak.getCartAbandonmentLossINR()),
                (leak.getOverallAbandonmentRate() != null ? leak.getOverallAbandonmentRate() * 100 : 0),
                formatINR(leak.getCheckoutDropLossINR()),
                worst.getName(),
                worst.getAbandonmentRate() * 100,
                formatINR(worst.getLeakINR()),
                topSourceName,
                topSourceAbandonment * 100
        );

        return callAi(prompt);
    }

    public String generateProductHealthInsight(ProductHealthResponse health) {
        if (health.getProducts() == null || health.getProducts().isEmpty()) {
            return null;
        }

        long listingProblems = health.getProducts().stream().filter(p -> "LISTING_PROBLEM".equals(p.getQuadrant())).count();
        long seoProblems = health.getProducts().stream().filter(p -> "SEO_PROBLEM".equals(p.getQuadrant())).count();
        long winners = health.getProducts().stream().filter(p -> "WINNER".equals(p.getQuadrant())).count();
        long wrongPlatform = health.getProducts().stream().filter(p -> "WRONG_PLATFORM".equals(p.getQuadrant())).count();

        String topAction = health.getActionQueue().isEmpty()
                ? "no immediate action needed"
                : health.getActionQueue().get(0).getDescription();

        String prompt = """
                Store: %s — Product Health Matrix (last 30 days):
                - Total products: %d
                - Winners (high traffic + high conversion): %d
                - Listing problems (high traffic, low conversion): %d
                - SEO problems (low traffic, high conversion): %d
                - Wrong platform (low traffic + low conversion): %d
                - Top priority action: %s

                Write exactly 2 sentences as instructed.
                """.formatted(
                health.getStoreName(),
                health.getProducts().size(),
                winners, listingProblems, seoProblems, wrongPlatform,
                topAction
        );

        return callAi(prompt);
    }

    public String generateSeoGapInsight(SeoGapResponse seo) {
        if (seo.getOpportunities() == null || seo.getOpportunities().isEmpty()) {
            return null;
        }

        SeoGapResponse.KeywordOpportunity topOpp = seo.getOpportunities().get(0);

        String prompt = """
                Store: %s — SEO Gap Analysis:
                - Total monthly revenue opportunity if gaps are closed: ₹%s
                - Number of "almost ranking" keywords: %d
                - Biggest opportunity: "%s" currently at position %.1f
                  → moving to position 3 would add +%d clicks/month = +₹%s/month
                - This product is named: "%s"

                Write exactly 2 sentences as instructed.
                """.formatted(
                seo.getStoreName(),
                formatINR(seo.getTotalOpportunityINR()),
                seo.getOpportunities().size(),
                topOpp.getQuery(),
                topOpp.getCurrentPosition(),
                topOpp.getEstimatedNewClicksPerMonth(),
                formatINR(topOpp.getEstimatedRevenuePerMonth()),
                topOpp.getProductName()
        );

        return callAi(prompt);
    }

    private String callAi(String userPrompt) {
        try {
            return openAiClient.callTextPlain(SYSTEM_PROMPT, userPrompt).getText();
        } catch (Exception e) {
            log.warn("AI insight generation failed: {}", e.getMessage());
            return null;
        }
    }

    private String formatINR(java.math.BigDecimal value) {
        if (value == null) return "0";
        return String.format("%.0f", value);
    }
}
