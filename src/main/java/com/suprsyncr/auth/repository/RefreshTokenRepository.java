package com.suprsyncr.auth.repository;

import com.suprsyncr.auth.entity.RefreshToken;
import com.suprsyncr.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for RefreshToken entity.
 * Provides database access methods for refresh token management.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    /**
     * Finds a refresh token by its token string.
     *
     * @param token the token string to search for
     * @return an Optional containing the refresh token if found, empty otherwise
     */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * Deletes all refresh tokens associated with a user.
     * Used during logout to revoke all user sessions.
     *
     * @param user the user whose tokens should be deleted
     */
    void deleteByUser(User user);
}

