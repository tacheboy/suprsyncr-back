package com.suprsyncr.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.ai.client.OpenAiClient;
import com.suprsyncr.ai.client.OpenAiResponse;
import com.suprsyncr.ai.context.SellerContextBuilder;
import com.suprsyncr.ai.domain.AiInsightsCache;
import com.suprsyncr.ai.domain.AiRequest;
import com.suprsyncr.ai.dto.InsightResponse;
import com.suprsyncr.ai.exception.AiParseException;
import com.suprsyncr.ai.prompt.InsightsPrompt;
import com.suprsyncr.ai.repository.AiInsightsCacheRepository;
import com.suprsyncr.ai.repository.AiRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightsService {

    private final OpenAiClient openAiClient;
    private final SellerContextBuilder contextBuilder;
    private final AiInsightsCacheRepository cacheRepository;
    private final AiRequestRepository requestRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public InsightResponse getWeeklyInsights(Long sellerId, boolean forceRefresh) {
        LocalDate lastMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return getOrGenerateInsight(sellerId, "WEEKLY", lastMonday, 7, forceRefresh);
    }

    @Transactional
    public InsightResponse getMonthlyInsights(Long sellerId, boolean forceRefresh) {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        return getOrGenerateInsight(sellerId, "MONTHLY", firstOfMonth, 30, forceRefresh);
    }

    private InsightResponse getOrGenerateInsight(Long sellerId, String type, LocalDate periodStart, int days, boolean forceRefresh) {
        Optional<AiInsightsCache> cached = cacheRepository.findBySellerIdAndInsightTypeAndPeriodStart(sellerId, type, periodStart);

        if (!forceRefresh && cached.isPresent()) {
            try {
                return objectMapper.readValue(cached.get().getContent(), InsightResponse.class);
            } catch (Exception e) {
                log.warn("Failed to parse cached insight for seller {}", sellerId, e);
            }
        }

        if (forceRefresh && cached.isPresent()) {
            cacheRepository.delete(cached.get());
        }

        String richContext = contextBuilder.buildRichContext(sellerId, days);

        if (richContext.contains("No order data available")) {
            return InsightResponse.createPlaceholder(
                "Welcome to Insights",
                "Once you start receiving orders, your AI dashboard will populate with actionable intel.",
                type
            );
        }

        String system = InsightsPrompt.system();
        String user = InsightsPrompt.user(richContext, type);

        OpenAiResponse response;
        try {
            response = openAiClient.callText(system, user);
        } catch (Exception e) {
            log.error("Failed to call OpenAI for insights", e);
            throw new RuntimeException("Could not generate insights", e);
        }

        try {
            InsightResponse insightData = objectMapper.readValue(response.getText(), InsightResponse.class);
            insightData.setPeriodStart(periodStart.toString());
            insightData.setPeriod(type);

            String jsonContent = objectMapper.writeValueAsString(insightData);
            cacheRepository.save(AiInsightsCache.builder()
                    .sellerId(sellerId)
                    .insightType(type)
                    .periodStart(periodStart)
                    .periodEnd(periodStart.plusDays(days - 1))
                    .content(jsonContent)
                    .generatedAt(LocalDateTime.now())
                    .build());

            saveAiRequest(sellerId, "INSIGHTS_" + type, "Generated " + type + " insights", response, "SUCCESS");
            return insightData;

        } catch (Exception e) {
            saveAiRequest(sellerId, "INSIGHTS_" + type, "Failed to parse " + type, response, "FAILED");
            log.error("Failed to parse insight response", e);
            throw new AiParseException("Could not parse insight JSON", e);
        }
    }

    private void saveAiRequest(Long sellerId, String feature, String summary, OpenAiResponse response, String status) {
        AiRequest aiReq = AiRequest.builder()
                .sellerId(sellerId)
                .feature(feature)
                .inputSummary(summary)
                .status(status)
                .build();

        if (response != null) {
            aiReq.setGeminiModel("gpt-4o");
            aiReq.setPromptTokens(response.getPromptTokens());
            aiReq.setOutputTokens(response.getOutputTokens());
            aiReq.setLatencyMs((int) response.getLatencyMs());
        }

        requestRepository.save(aiReq);
    }
}
