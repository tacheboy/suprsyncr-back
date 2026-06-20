package com.suprsyncr.auth.controller;

import com.suprsyncr.auth.dto.*;
import com.suprsyncr.auth.entity.User;
import com.suprsyncr.auth.service.AuthService;
import com.suprsyncr.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST controller for authentication endpoints.
 * Handles user registration, login, token refresh, logout, and current user retrieval.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    /**
     * Registers a new user account.
     * 
     * @param request the registration request with user details
     * @return ApiResponse containing authentication tokens and user information
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with encrypted password and returns authentication tokens")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or email already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        ApiResponse<AuthResponse> apiResponse = new ApiResponse<>(
                true,
                response,
                "User registered successfully",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }
    
    /**
     * Authenticates a user and generates tokens.
     * 
     * @param request the login request with credentials
     * @return ApiResponse containing authentication tokens and user information
     */
    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates user credentials and returns access and refresh tokens")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        ApiResponse<AuthResponse> apiResponse = new ApiResponse<>(
                true,
                response,
                "Login successful",
                LocalDateTime.now()
        );
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Refreshes an access token using a valid refresh token.
     * 
     * @param request the refresh token request
     * @return ApiResponse containing new access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generates a new access token using a valid refresh token")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        ApiResponse<AuthResponse> apiResponse = new ApiResponse<>(
                true,
                response,
                "Token refreshed successfully",
                LocalDateTime.now()
        );
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Logs out a user by revoking their refresh token.
     * 
     * @param request the refresh token request
     * @return ApiResponse with no data
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes the refresh token to log out the user")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid refresh token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        ApiResponse<Void> apiResponse = new ApiResponse<>(
                true,
                null,
                "Logout successful",
                LocalDateTime.now()
        );
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Gets the currently authenticated user's information.
     * Requires authentication.
     * 
     * @return ApiResponse containing current user information
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user", description = "Retrieves the currently authenticated user's profile information")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        User user = authService.getCurrentUser();
        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        ApiResponse<UserDto> apiResponse = new ApiResponse<>(
                true,
                userDto,
                "User retrieved successfully",
                LocalDateTime.now()
        );
        return ResponseEntity.ok(apiResponse);
    }
}

