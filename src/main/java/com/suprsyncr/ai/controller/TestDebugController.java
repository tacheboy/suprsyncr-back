package com.suprsyncr.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.ai.client.OpenAiClient;
import com.suprsyncr.ai.client.OpenAiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test/debug")
@RequiredArgsConstructor
public class TestDebugController {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    @GetMapping("/ai-raw")
    public String debugAiRaw(@RequestParam(defaultValue = "Say hello in JSON format: {\"greeting\": \"...\"}") String prompt) {
        try {
            OpenAiResponse response = openAiClient.callText("You are a helpful assistant.", prompt);
            JsonNode parsed = objectMapper.readTree(response.getText());
            return "=== OpenAI Response ===\n"
                    + "Latency: " + response.getLatencyMs() + "ms\n"
                    + "Tokens: " + response.getPromptTokens() + " in / " + response.getOutputTokens() + " out\n\n"
                    + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception e) {
            return "EXCEPTION: " + e.getMessage();
        }
    }
}
