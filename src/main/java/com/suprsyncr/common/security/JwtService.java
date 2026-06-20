package com.suprsyncr.common.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Service interface for JWT token operations.
 * Handles generation, validation, and extraction of JWT tokens.
 */
public interface JwtService {
    
    /**
     * Generates an access token for the given user.
     * Access tokens are short-lived (15 minutes) and used for API authentication.
     *
     * @param userDetails the user details
     * @return the generated access token
     */
    String generateAccessToken(UserDetails userDetails);
    
    /**
     * Generates a refresh token for the given user.
     * Refresh tokens are long-lived (7 days) and used to obtain new access tokens.
     *
     * @param userDetails the user details
     * @return the generated refresh token
     */
    String generateRefreshToken(UserDetails userDetails);
    
    /**
     * Extracts the username from the given token.
     *
     * @param token the JWT token
     * @return the username (subject) from the token
     */
    String extractUsername(String token);
    
    /**
     * Validates if the token is valid for the given user.
     * Checks both signature validity and username match.
     *
     * @param token the JWT token
     * @param userDetails the user details to validate against
     * @return true if the token is valid, false otherwise
     */
    boolean isTokenValid(String token, UserDetails userDetails);
    
    /**
     * Checks if the token has expired.
     *
     * @param token the JWT token
     * @return true if the token is expired, false otherwise
     */
    boolean isTokenExpired(String token);
}

