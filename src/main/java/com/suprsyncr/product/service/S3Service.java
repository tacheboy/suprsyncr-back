package com.suprsyncr.product.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

/**
 * Service for handling S3 image uploads.
 */
@Service
public class S3Service {
    
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String s3Endpoint;
    
    public S3Service(
        S3Presigner s3Presigner,
        @Value("${aws.s3.bucket-name}") String bucketName,
        @Value("${aws.s3.endpoint:}") String s3Endpoint
    ) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.s3Endpoint = s3Endpoint;
    }
    
    /**
     * Generate a pre-signed URL for uploading an image to S3.
     * 
     * @param fileName the original file name
     * @param contentType the content type of the file
     * @return the pre-signed upload URL and the final public URL
     */
    public PresignedUrlResult generatePresignedUploadUrl(String fileName, String contentType) {
        // Generate unique key for the file
        String key = "products/" + UUID.randomUUID() + "-" + fileName;
        
        // Create the PutObjectRequest
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build();
        
        // Create the presign request with 15-minute expiration
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(15))
            .putObjectRequest(putObjectRequest)
            .build();
        
        // Generate the presigned URL
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presignedRequest.url().toString();
        
        // Generate the public URL
        String publicUrl = getPublicUrl(key);
        
        return new PresignedUrlResult(uploadUrl, publicUrl, key);
    }
    
    /**
     * Get the public URL for an S3 object.
     * 
     * @param key the S3 object key
     * @return the public URL
     */
    public String getPublicUrl(String key) {
        if (s3Endpoint != null && !s3Endpoint.isEmpty()) {
            // LocalStack or custom endpoint
            return s3Endpoint + "/" + bucketName + "/" + key;
        } else {
            // AWS S3
            return String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
        }
    }
    
    /**
     * Result of presigned URL generation.
     */
    public record PresignedUrlResult(String uploadUrl, String publicUrl, String key) {}
}

