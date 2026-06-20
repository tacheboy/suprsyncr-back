package com.suprsyncr.autopilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

/**
 * Configuration for the Autopilot subsystem.
 * Provides RestTemplate for Python service communication
 * and an async executor for background agent runs.
 */
@Configuration
@EnableAsync
public class AutopilotConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Async executor for agent runs.
     * Agent runs can take 30-60 seconds (multiple LLM calls),
     * so they must not block the HTTP request thread.
     */
    @Bean(name = "agentRunExecutor")
    public Executor agentRunExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("agent-run-");
        executor.initialize();
        return executor;
    }
}

