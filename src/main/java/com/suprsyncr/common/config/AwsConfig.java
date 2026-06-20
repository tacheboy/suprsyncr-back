package com.suprsyncr.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * AWS S3 configuration for product image storage.
 * Supports both AWS S3 and LocalStack for development.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${aws.s3.access-key-id}")
    private String accessKeyId;

    @Value("${aws.s3.secret-access-key}")
    private String secretAccessKey;

    /**
     * Creates AWS credentials provider from environment variables.
     */
    private AwsCredentialsProvider credentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        return StaticCredentialsProvider.create(credentials);
    }

    /**
     * Creates S3Client bean with LocalStack endpoint override support.
     * Uses path-style bucket access when endpoint is configured (LocalStack).
     */
    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());

        // Configure endpoint for LocalStack if provided
        if (s3Endpoint != null && !s3Endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(s3Endpoint))
                   .forcePathStyle(true); // Required for LocalStack
        }

        return builder.build();
    }

    /**
     * Creates S3Presigner bean for generating pre-signed URLs.
     * Supports LocalStack endpoint override for development.
     */
    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());

        // Configure endpoint for LocalStack if provided
        if (s3Endpoint != null && !s3Endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(s3Endpoint));
        }

        return builder.build();
    }
}

