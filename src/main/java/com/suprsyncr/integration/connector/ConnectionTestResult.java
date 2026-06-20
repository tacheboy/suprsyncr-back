package com.suprsyncr.integration.connector;

import java.util.Map;

/**
 * Result of a platform connection test.
 */
public record ConnectionTestResult(
    boolean success,
    String message,
    Map<String, Object> metadata
) {}
