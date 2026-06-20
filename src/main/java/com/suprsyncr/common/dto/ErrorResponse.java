package com.suprsyncr.common.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
    String error,
    String message,
    int status,
    LocalDateTime timestamp,
    List<String> details
) {
    public static ErrorResponse of(String error, String message, int status) {
        return new ErrorResponse(error, message, status, LocalDateTime.now(), null);
    }
    
    public static ErrorResponse of(String error, String message, int status, List<String> details) {
        return new ErrorResponse(error, message, status, LocalDateTime.now(), details);
    }
}

