package com.suprsyncr.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.ai.config.OpenAiConfig;
import com.suprsyncr.ai.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiConfig config;

    public OpenAiResponse callText(String system, String user) {
        List<Map<String, Object>> messages = buildMessages(system, user);
        return execute(messages, true);
    }

    public OpenAiResponse callTextPlain(String system, String user) {
        List<Map<String, Object>> messages = buildMessages(system, user);
        return execute(messages, false);
    }

    public OpenAiResponse callVision(String system, String user, byte[] imageBytes, String mimeType) {
        List<Map<String, Object>> messages = buildVisionMessages(system, user, imageBytes, mimeType);
        return execute(messages, true);
    }

    public OpenAiResponse callChat(String system, List<Map<String, Object>> history, String userMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (system != null) {
            messages.add(Map.of("role", "system", "content", system));
        }
        // history is already in OpenAI format (role + content)
        messages.addAll(history);
        messages.add(Map.of("role", "user", "content", userMessage));
        return execute(messages, false);
    }

    private List<Map<String, Object>> buildMessages(String system, String user) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (system != null) {
            messages.add(Map.of("role", "system", "content", system));
        }
        messages.add(Map.of("role", "user", "content", user));
        return messages;
    }

    private List<Map<String, Object>> buildVisionMessages(String system, String user, byte[] imageBytes, String mimeType) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (system != null) {
            messages.add(Map.of("role", "system", "content", system));
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        List<Map<String, Object>> contentParts = new ArrayList<>();
        contentParts.add(Map.of("type", "text", "text", user));
        contentParts.add(Map.of(
            "type", "image_url",
            "image_url", Map.of("url", "data:" + mimeType + ";base64," + base64Image)
        ));

        messages.add(Map.of("role", "user", "content", contentParts));
        return messages;
    }

    private OpenAiResponse execute(List<Map<String, Object>> messages, boolean jsonMode) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", config.getModel());
        payload.put("messages", messages);
        payload.put("temperature", config.getTemperature());
        payload.put("max_tokens", config.getMaxOutputTokens());
        if (jsonMode) {
            payload.put("response_format", Map.of("type", "json_object"));
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            Request request = new Request.Builder()
                    .url(config.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(jsonPayload, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                long latencyMs = System.currentTimeMillis() - startTime;

                if (!response.isSuccessful()) {
                    handleErrorResponse(response);
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);

                String text = root.path("choices").get(0)
                        .path("message").path("content").asText("").trim();

                JsonNode usage = root.path("usage");
                int promptTokens = usage.path("prompt_tokens").asInt(0);
                int outputTokens = usage.path("completion_tokens").asInt(0);

                log.debug("OpenAI response (model={}): {} chars in {}ms", config.getModel(), text.length(), latencyMs);

                return OpenAiResponse.builder()
                        .text(text)
                        .promptTokens(promptTokens)
                        .outputTokens(outputTokens)
                        .latencyMs(latencyMs)
                        .build();
            }
        } catch (IOException e) {
            throw new AiNetworkException("Network error calling OpenAI", e);
        }
    }

    private void handleErrorResponse(Response response) throws IOException {
        int code = response.code();
        String body = response.body() != null ? response.body().string() : "";
        log.error("OpenAI API Error {}: {}", code, body);

        if (code == 429) {
            throw new AiRateLimitException("OpenAI rate limit exceeded");
        } else if (code == 401 || code == 403) {
            throw new AiAuthException("Invalid OpenAI API key or access denied");
        } else if (code >= 500) {
            throw new AiServerException("OpenAI server error");
        } else {
            throw new AiException("Unexpected OpenAI error: " + code);
        }
    }
}
