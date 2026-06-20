package com.suprsyncr.auth.service;

import com.suprsyncr.auth.dto.*;
import com.suprsyncr.auth.entity.RefreshToken;
import com.suprsyncr.auth.entity.User;
import com.suprsyncr.auth.entity.UserRole;
import com.suprsyncr.auth.repository.RefreshTokenRepository;
import com.suprsyncr.auth.repository.UserRepository;
import com.suprsyncr.common.exception.UnauthorizedException;
import com.suprsyncr.common.exception.ValidationException;
import com.suprsyncr.common.security.JwtService;
import com.suprsyncr.seller.entity.Seller;
import com.suprsyncr.seller.entity.SellerStatus;
import com.suprsyncr.seller.repository.SellerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of AuthService.
 * Handles user authentication, registration, and token management.
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {
    
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            SellerRepository sellerRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.sellerRepository = sellerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Ensures every authenticated user has a Seller profile. The entire product is
     * seller-scoped (AI tools, analytics, autopilot, products, orders), so a missing
     * Seller row makes those endpoints fail with "Seller profile not found". A minimal
     * profile is created with placeholder details the user can edit in Seller Profile.
     */
    private void ensureSellerExists(User user) {
        if (sellerRepository.findByUserId(user.getId()).isPresent()) {
            return;
        }
        Seller seller = new Seller();
        seller.setUser(user);
        String name = (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName() + "'s Store"
                : "My Store";
        seller.setBusinessName(name);
        seller.setBusinessAddress("Not provided");
        seller.setPhoneNumber("Not provided");
        seller.setStatus(SellerStatus.ACTIVE);
        sellerRepository.save(seller);
        log.info("Auto-provisioned seller profile for userId: {}", user.getId());
    }
    
    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.email());
        
        // Validate email uniqueness
        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("Registration failed - email already exists: {}", request.email());
            throw new ValidationException("Email already registered");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(UserRole.SELLER);
        user.setEnabled(true);
        
        user = userRepository.save(user);

        // Every user is a seller — provision their profile up-front so seller-scoped
        // features (AI tools, analytics, autopilot) work immediately after signup.
        ensureSellerExists(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenString = UUID.randomUUID().toString();

        // Save refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenString);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        log.info("Registration successful for email: {}, userId: {}", request.email(), user.getId());
        
        // Build response
        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        return new AuthResponse(accessToken, refreshTokenString, "Bearer", accessTokenExpiration / 1000, userDto);
    }
    
    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());
        
        try {
            // Authenticate credentials
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            
            User user = (User) authentication.getPrincipal();

            // Backfill a seller profile for accounts created before auto-provisioning
            // existed, so existing users get seller-scoped features without re-registering.
            ensureSellerExists(user);

            // Generate tokens
            String accessToken = jwtService.generateAccessToken(user);
            String refreshTokenString = UUID.randomUUID().toString();
            
            // Save refresh token
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(refreshTokenString);
            refreshToken.setUser(user);
            refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));
            refreshToken.setRevoked(false);
            refreshTokenRepository.save(refreshToken);
            
            log.info("Login successful for email: {}, userId: {}", request.email(), user.getId());
            
            // Build response
            UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
            return new AuthResponse(accessToken, refreshTokenString, "Bearer", accessTokenExpiration / 1000, userDto);
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            log.warn("Login failed for email: {} - invalid credentials", request.email());
            throw new UnauthorizedException("Incorrect email or password. Please try again.");
        } catch (Exception e) {
            log.warn("Login failed for email: {} - {}", request.email(), e.getMessage());
            throw e;
        }
    }
    
    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Find and validate refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        
        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }
        
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }
        
        // Generate new access token
        User user = refreshToken.getUser();
        String accessToken = jwtService.generateAccessToken(user);
        
        // Build response (reuse existing refresh token)
        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        return new AuthResponse(accessToken, request.refreshToken(), "Bearer", accessTokenExpiration / 1000, userDto);
    }
    
    @Override
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }
    
    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}

