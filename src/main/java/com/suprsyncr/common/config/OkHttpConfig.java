package com.suprsyncr.common.config;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * OkHttp client configuration for marketplace API integrations.
 * Configures timeouts and retry behavior for reliable HTTP communication.
 */
@Configuration
public class OkHttpConfig {

    @Value("${http.client.connect-timeout:30000}")
    private long connectTimeout;

    @Value("${http.client.read-timeout:30000}")
    private long readTimeout;

    @Value("${http.client.write-timeout:30000}")
    private long writeTimeout;

    /**
     * Creates OkHttpClient bean with configured timeouts and retry on connection failure.
     * All timeouts are set to 30 seconds by default.
     */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .readTimeout(Duration.ofMillis(readTimeout))
                .writeTimeout(Duration.ofMillis(writeTimeout))
                .retryOnConnectionFailure(true) // Retry on connection failure
                .build();
    }
}

