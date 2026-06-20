package com.suprsyncr.ai.client;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OpenAiResponse {
    private String text;
    private int promptTokens;
    private int outputTokens;
    private long latencyMs;
}
