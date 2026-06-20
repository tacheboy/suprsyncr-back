package com.suprsyncr.common.exception;

import com.suprsyncr.ai.exception.AiAuthException;
import com.suprsyncr.ai.exception.AiException;
import com.suprsyncr.ai.exception.AiNetworkException;
import com.suprsyncr.ai.exception.AiParseException;
import com.suprsyncr.ai.exception.AiRateLimitException;
import com.suprsyncr.ai.exception.AiServerException;
import com.suprsyncr.common.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("Resource Not Found", ex.getMessage(), 404));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Validation Error", ex.getMessage(), 400));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> details.add(e.getField() + ": " + e.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Validation Error", "Invalid request parameters", 400, details));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("Unauthorized", ex.getMessage(), 401));
    }

    @ExceptionHandler(MarketplaceApiException.class)
    public ResponseEntity<ErrorResponse> handleMarketplaceApi(MarketplaceApiException ex) {
        log.error("Marketplace API error - Platform: {}, Status: {}", ex.getPlatformType(), ex.getStatusCode(), ex);
        List<String> details = List.of("Platform: " + ex.getPlatformType());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of("Marketplace API Error", ex.getMessage(), 502, details));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        List<String> details = List.of(
            "Product Variant ID: " + ex.getProductVariantId(),
            "Requested: " + ex.getRequested(),
            "Available: " + ex.getAvailable()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("Insufficient Stock", ex.getMessage(), 409, details));
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransition(InvalidStateTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("Invalid State Transition", ex.getMessage(), 409));
    }

    @ExceptionHandler(UnsupportedPlatformException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedPlatform(UnsupportedPlatformException ex) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ErrorResponse.of("Unsupported Platform", ex.getMessage(), 501,
                        List.of("Platform: " + ex.getPlatformType())));
    }

    @ExceptionHandler(AiAuthException.class)
    public ResponseEntity<ErrorResponse> handleAiAuth(AiAuthException ex) {
        log.error("OpenAI authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("AI Authentication Error",
                        "OpenAI API key is invalid or missing. Set OPENAI_API_KEY in your environment.", 401));
    }

    @ExceptionHandler(AiRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleAiRateLimit(AiRateLimitException ex) {
        log.warn("OpenAI rate limit hit: {}", ex.getMessage());
        return ResponseEntity.status(429)
                .body(ErrorResponse.of("AI Rate Limited", "OpenAI quota exceeded. Please wait and try again.", 429));
    }

    @ExceptionHandler({AiNetworkException.class, AiServerException.class})
    public ResponseEntity<ErrorResponse> handleAiNetwork(AiException ex) {
        log.error("OpenAI network/server error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of("AI Service Unavailable", "Could not reach OpenAI API. Please try again.", 502));
    }

    @ExceptionHandler(AiParseException.class)
    public ResponseEntity<ErrorResponse> handleAiParse(AiParseException ex) {
        log.error("Failed to parse AI response: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of("AI Processing Error",
                        "AI response could not be processed. Please try again.", 422));
    }

    @ExceptionHandler(AiException.class)
    public ResponseEntity<ErrorResponse> handleAiGeneric(AiException ex) {
        log.error("AI error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("AI Error", ex.getMessage(), 500));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("Internal Server Error", ex.getMessage(), 500));
    }
}
