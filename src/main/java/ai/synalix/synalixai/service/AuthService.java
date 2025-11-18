package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.auth.LoginResponse;
import ai.synalix.synalixai.entity.RefreshToken;
import ai.synalix.synalixai.entity.User;
import ai.synalix.synalixai.enums.AuditOperationType;

import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.RefreshTokenRepository;
import ai.synalix.synalixai.repository.UserRepository;
import ai.synalix.synalixai.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication service for user login and token management
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    @Autowired
    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    /**
     * Login user with username and password
     */
    @Transactional
    public LoginResponse login(String username, String password, String ipAddress, String userAgent) {
        Map<String, String> tokens = authenticate(username, password, ipAddress);
        
        // Get access token expiration time (5 minutes in seconds)
        long expiresIn = 5L * 60; // 5 minutes
        
        return new LoginResponse(
            tokens.get("accessToken"),
            tokens.get("refreshToken"),
            tokens.get("tokenType"),
            expiresIn
        );
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public LoginResponse refreshToken(String refreshTokenValue, String ipAddress, String userAgent) {
        Map<String, String> tokens = refreshAccessToken(refreshTokenValue);
        
        // Get access token expiration time (5 minutes in seconds)
        long expiresIn = 5L * 60; // 5 minutes
        
        return new LoginResponse(
            tokens.get("accessToken"),
            null, // Don't return refresh token on refresh
            tokens.get("tokenType"),
            expiresIn
        );
    }

    /**
     * Logout user by revoking refresh token
     */
    @Transactional
    public void logout(String refreshTokenValue, String ipAddress, String userAgent) {
        // Find user ID from refresh token
        UUID userId = null;
        if (refreshTokenValue != null) {
            Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(refreshTokenValue);
            if (refreshToken.isPresent()) {
                userId = refreshToken.get().getUserId();
            }
        }
        
        logout(refreshTokenValue, userId);
    }

    /**
     * Authenticate user with username and password
     */
    @Transactional
    public Map<String, String> authenticate(String username, String password, String ipAddress) {
        try {
            // Find user by username
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ApiException(ApiErrorCode.INVALID_CREDENTIALS));

            // Check if user is enabled
            if (!user.isEnabled()) {
                auditService.logUserAuthentication(AuditOperationType.USER_LOGIN, user.getId(), username, ipAddress, false);
                throw new ApiException(ApiErrorCode.ACCOUNT_DISABLED);
            }

            // Verify password
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                auditService.logUserAuthentication(AuditOperationType.USER_LOGIN, user.getId(), username, ipAddress, false);
                throw new ApiException(ApiErrorCode.INVALID_CREDENTIALS);
            }

            // Generate tokens
            String accessToken = jwtUtil.generateAccessToken(user);
            String refreshToken = generateAndSaveRefreshToken(user);

            // Log successful authentication
            auditService.logUserAuthentication(AuditOperationType.USER_LOGIN, user.getId(), username, ipAddress, true);

            logger.info("User authenticated successfully: username={}, userId={}", username, user.getId());

            return Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "tokenType", "Bearer"
            );

        } catch (ApiException e) {
            logger.warn("Authentication failed for username: {}, reason: {}", username, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Authentication error for username: {}", username, e);
            throw new ApiException(ApiErrorCode.AUTHENTICATION_SERVICE_ERROR);
        }
    }

    /**
     * Refresh access token using refresh token (internal method)
     */
    @Transactional
    public Map<String, String> refreshAccessToken(String refreshTokenValue) {
        try {
            // Find refresh token
            RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                    .orElseThrow(() -> new ApiException(ApiErrorCode.INVALID_REFRESH_TOKEN));

            // Check if token is valid
            if (!refreshToken.isValid()) {
                auditService.logTokenEvent(AuditOperationType.TOKEN_REFRESH, refreshToken.getUserId(), "refresh", "failed_invalid");
                throw new ApiException(ApiErrorCode.INVALID_REFRESH_TOKEN);
            }

            // Get user
            User user = userRepository.findById(refreshToken.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // Check if user is still enabled
            if (!user.isEnabled()) {
                auditService.logTokenEvent(AuditOperationType.TOKEN_REFRESH, user.getId(), "refresh", "failed_disabled");
                throw new ApiException(ApiErrorCode.ACCOUNT_DISABLED);
            }

            // Generate new access token
            String newAccessToken = jwtUtil.generateAccessToken(user);

            // Log token refresh
            auditService.logTokenEvent(AuditOperationType.TOKEN_REFRESH, user.getId(), "access", "success");

            logger.info("Token refreshed successfully for user: {}", user.getId());

            return Map.of(
                    "accessToken", newAccessToken,
                    "tokenType", "Bearer"
            );

        } catch (ApiException e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Token refresh error", e);
            throw new ApiException(ApiErrorCode.AUTHENTICATION_SERVICE_ERROR);
        }
    }

    /**
     * Logout user by revoking refresh token
     */
    @Transactional
    public void logout(String refreshTokenValue, UUID userId) {
        try {
            if (refreshTokenValue != null) {
                refreshTokenRepository.revokeTokenByToken(refreshTokenValue);
                auditService.logTokenEvent(AuditOperationType.TOKEN_REVOKE, userId, "refresh", "logout");
            }
            
            if (userId != null) {
                auditService.logUserAuthentication(AuditOperationType.USER_LOGOUT, userId, null, null, true);
            }

            logger.info("User logged out successfully: userId={}", userId);

        } catch (Exception e) {
            logger.error("Logout error for userId: {}", userId, e);
        }
    }

    /**
     * Revoke all refresh tokens for a user
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        try {
            int revokedCount = refreshTokenRepository.revokeAllTokensByUserId(userId);
            auditService.logTokenEvent(AuditOperationType.TOKEN_REVOKE, userId, "refresh", "all_revoked");
            
            logger.info("Revoked {} refresh tokens for user: {}", revokedCount, userId);

        } catch (Exception e) {
            logger.error("Error revoking tokens for userId: {}", userId, e);
            throw new RuntimeException("Failed to revoke user tokens");
        }
    }

    /**
     * Generate and save refresh token
     */
    private String generateAndSaveRefreshToken(User user) {
        // Generate random token
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String tokenValue = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Calculate expiration time (7 days from now)
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        // Save to database
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(tokenValue);
        refreshToken.setExpiresAt(expiresAt);
        refreshTokenRepository.save(refreshToken);

        return tokenValue;
    }

    /**
     * Validate refresh token
     */
    public boolean isValidRefreshToken(String tokenValue) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(tokenValue);
        return tokenOpt.isPresent() && tokenOpt.get().isValid();
    }

    /**
     * Get user from access token
     */
    public Optional<User> getUserFromAccessToken(String accessToken) {
        try {
            if (!jwtUtil.validateToken(accessToken) || !jwtUtil.isAccessToken(accessToken)) {
                return Optional.empty();
            }

            String userId = jwtUtil.extractUserId(accessToken);
            return userRepository.findById(UUID.fromString(userId));

        } catch (Exception e) {
            logger.debug("Failed to get user from access token: {}", e.getMessage());
            return Optional.empty();
        }
    }
}