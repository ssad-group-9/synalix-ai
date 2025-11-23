package ai.synalix.synalixai.util;

import ai.synalix.synalixai.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT utility class for token generation and validation
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    /**
     * Get signing key from secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate access token for user
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().name());
        claims.put("status", user.getStatus().name());
        claims.put("type", "access");
        
        return createToken(claims, user.getUsername(), accessTokenExpiration);
    }

    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("username", user.getUsername());
        claims.put("type", "refresh");
        
        return createToken(claims, user.getUsername(), refreshTokenExpiration);
    }

    /**
     * Create JWT token with claims and expiration
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        var now = new Date();
        var expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from token
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    /**
     * Extract user role from token
     */
    public String extractUserRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extract user status from token
     */
    public String extractUserStatus(String token) {
        return extractClaim(token, claims -> claims.get("status", String.class));
    }

    /**
     * Extract token type from token
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final var claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            logger.error("JWT token malformed: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            logger.error("JWT signature invalid: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("JWT token illegal argument: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Check if token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Validate token against user
     */
    public Boolean validateToken(String token, User user) {
        try {
            final var username = extractUsername(token);
            final var userId = extractUserId(token);
            
            return (username.equals(user.getUsername()) && 
                    userId.equals(user.getId().toString()) && 
                    !isTokenExpired(token));
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token without user context
     */
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is access token
     */
    public Boolean isAccessToken(String token) {
        try {
            return "access".equals(extractTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if token is refresh token
     */
    public Boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(extractTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }
}