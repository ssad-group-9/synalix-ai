package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.dto.auth.LoginRequest;
import ai.synalix.synalixai.dto.auth.LoginResponse;
import ai.synalix.synalixai.dto.auth.RefreshResponse;
import ai.synalix.synalixai.dto.auth.RefreshTokenRequest;
import ai.synalix.synalixai.service.AuthService;
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
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {

        var loginResponse = authService.login(
            loginRequest.getUsername(), 
            loginRequest.getPassword()
        );
        
        return ResponseEntity.ok(loginResponse);
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        
        var loginResponse = authService.refreshToken(
            refreshTokenRequest.getRefreshToken()
        );
        
        return ResponseEntity.ok(loginResponse);
    }

    /**
     * User logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        
        authService.logout(
            refreshTokenRequest.getRefreshToken()
        );
        
        return ResponseEntity.ok(null);
    }

}