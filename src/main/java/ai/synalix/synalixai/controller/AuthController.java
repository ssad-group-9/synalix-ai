package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.dto.auth.LoginRequest;
import ai.synalix.synalixai.dto.auth.LoginResponse;
import ai.synalix.synalixai.dto.auth.RefreshTokenRequest;
import ai.synalix.synalixai.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST controller
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * User login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest, 
            HttpServletRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        LoginResponse loginResponse = authService.login(
            loginRequest.getUsername(), 
            loginRequest.getPassword(),
            ipAddress,
            userAgent
        );
        
        return ResponseEntity.ok(loginResponse);
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        LoginResponse loginResponse = authService.refreshToken(
            refreshTokenRequest.getRefreshToken(),
            ipAddress,
            userAgent
        );
        
        return ResponseEntity.ok(loginResponse);
    }

    /**
     * User logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        authService.logout(
            refreshTokenRequest.getRefreshToken(),
            ipAddress,
            userAgent
        );
        
        return ResponseEntity.ok(null);
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}