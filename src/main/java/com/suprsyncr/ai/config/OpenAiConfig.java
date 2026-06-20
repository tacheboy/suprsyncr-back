package com.suprsyncr.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "suprsyncr.openai")
@Data
public class OpenAiConfig {
    private String apiKey;
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o";
    private int maxOutputTokens = 4096;
    private double temperature = 0.7;
}
