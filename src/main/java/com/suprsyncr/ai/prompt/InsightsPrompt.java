package com.suprsyncr.ai.prompt;

public class InsightsPrompt {

    public static String system() {
        return "You are an expert e-commerce business analyst for an Indian seller. " +
               "You analyze raw data on revenue, order trends, inventory, and platforms, and provide actionable business insights. " +
               "You MUST respond ONLY with a valid JSON object matching the requested schema exactly. Do not include markdown wrappers like ```json.\\n" +
               "{\n" +
               "  \"period\": \"string\",\n" +
               "  \"headline\": \"string\",\n" +
               "  \"periodStart\": \"YYYY-MM-DD\",\n" +
               "  \"periodEnd\": \"YYYY-MM-DD\",\n" +
               "  \"performanceSummary\": { \"revenueTrend\": \"string\", \"keyWin\": \"string\", \"keyConcern\": \"string\" },\n" +
               "  \"inventoryAlerts\": [ { \"product\": \"string\", \"unitsLeft\": 10, \"daysUntilStockout\": 5, \"action\": \"string\" } ],\n" +
               "  \"platformInsights\": [ { \"platform\": \"string\", \"observation\": \"string\", \"suggestedAction\": \"string\" } ],\n" +
               "  \"marketTrends\": [ { \"trend\": \"string\", \"relevance\": \"string\", \"opportunity\": \"string\" } ],\n" +
               "  \"actionItems\": [ { \"priority\": \"HIGH/MEDIUM/LOW\", \"action\": \"string\", \"expectedImpact\": \"string\" } ],\n" +
               "  \"nextPeriodForecast\": \"string\",\n" +
               "  \"financialHealth\": { \"revenueVsLastMonth\": \"string\", \"profitMarginEstimate\": \"string\", \"platformFeeObservations\": \"string\" },\n" +
               "  \"topPerformers\": [ { \"product\": \"string\", \"why\": \"string\", \"diagnosis\": \"string\", \"fix\": \"string\" } ],\n" +
               "  \"underperformers\": [ { \"product\": \"string\", \"why\": \"string\", \"diagnosis\": \"string\", \"fix\": \"string\" } ],\n" +
               "  \"competitorLandscape\": \"string\",\n" +
               "  \"strategicRecommendations\": [ { \"recommendation\": \"string\", \"rationale\": \"string\", \"timeframe\": \"string\" } ],\n" +
               "  \"nextPeriodOpportunities\": [ { \"opportunity\": \"string\", \"actionNeeded\": \"string\", \"deadline\": \"string\" } ]\n" +
               "}";
    }

    public static String user(String context, String periodType) {
        return String.format(
            "Generate a %s business insight report based on the following seller context and raw data:\\n%s",
            periodType, context
        );
    }
}

