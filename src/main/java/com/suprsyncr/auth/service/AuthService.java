package com.suprsyncr.auth.service;

import com.suprsyncr.auth.dto.*;
import com.suprsyncr.auth.entity.User;

/**
 * Service interface for authentication operations.
 * Handles user registration, login, token refresh, and logout.
 */
public interface AuthService {
    
    /**
     * Registers a new user with the provided details.
     * 
     * @param request the registration request containing user details
     * @return AuthResponse with tokens and user information
     */
    AuthResponse register(RegisterRequest request);
    
    /**
     * Authenticates a user and generates tokens.
     * 
     * @param request the login request containing credentials
     * @return AuthResponse with tokens and user information
     */
    AuthResponse login(LoginRequest request);
    
    /**
     * Refreshes an access token using a valid refresh token.
     * 
     * @param request the refresh token request
     * @return AuthResponse with new access token
     */
    AuthResponse refreshToken(RefreshTokenRequest request);
    
    /**
     * Logs out a user by revoking their refresh token.
     * 
     * @param refreshToken the refresh token to revoke
     */
    void logout(String refreshToken);
    
    /**
     * Gets the currently authenticated user.
     * 
     * @return the current user
     */
    User getCurrentUser();
}

