package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.dto.user.*;
import ai.synalix.synalixai.entity.User;
import ai.synalix.synalixai.enums.UserStatus;
import ai.synalix.synalixai.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User management REST controller
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create new user (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UUID operatorId = getCurrentUserId();
        
        // Generate random password if not provided
        String password = (request.getPassword() != null && !request.getPassword().trim().isEmpty()) 
            ? request.getPassword() 
            : generateRandomPassword();
        
        User createdUser = userService.createUser(
            request.getUsername(),
            password,
            request.getNickname(),
            request.getEmail(),
            request.getRole(),
            operatorId
        );
        
        UserResponse userResponse = convertToUserResponse(createdUser);
        CreateUserResponse response = new CreateUserResponse(userResponse, password);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all users (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponse> responses = users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        User user = userService.getUserById(userId);
        UserResponse response = convertToUserResponse(user);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        UUID currentUserId = getCurrentUserId();
        User user = userService.getUserById(currentUserId);
        UserResponse response = convertToUserResponse(user);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update user information
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        
        UUID operatorId = getCurrentUserId();
        User updatedUser;
        
        // Check if it's admin updating role/status or user updating own profile
        if (hasAdminRole() && (request.getRole() != null || request.getEnabled() != null)) {
            // Admin can update role and status
            updatedUser = userService.updateUserRole(userId, request.getRole(), operatorId);
            if (request.getEnabled() != null) {
                UserStatus newStatus = request.getEnabled() ? UserStatus.ENABLED : UserStatus.DISABLED;
                updatedUser = userService.updateUserStatus(userId, newStatus, operatorId);
            }
        }
        
        // Update basic information (nickname, email)
        if (request.getNickname() != null || request.getEmail() != null) {
            updatedUser = userService.updateUserInfo(userId, request.getNickname(), request.getEmail(), operatorId);
        } else {
            updatedUser = userService.getUserById(userId);
        }
        
        UserResponse response = convertToUserResponse(updatedUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Update current user profile
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUserProfile(
            @Valid @RequestBody UpdateUserRequest request) {
        
        UUID currentUserId = getCurrentUserId();
        
        // Regular users can only update nickname and email
        User updatedUser = userService.updateUserInfo(
            currentUserId, 
            request.getNickname(), 
            request.getEmail(), 
            currentUserId
        );
        
        UserResponse response = convertToUserResponse(updatedUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Change password
     */
    @PostMapping("/{userId}/change-password")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        userService.changePassword(
            userId, 
            request.getOldPassword(), 
            request.getNewPassword()
        );
        
        return ResponseEntity.ok(null);
    }

    /**
     * Change current user password
     */
    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changeCurrentUserPassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        
        UUID currentUserId = getCurrentUserId();
        
        userService.changePassword(
            currentUserId, 
            request.getOldPassword(), 
            request.getNewPassword()
        );
        
        return ResponseEntity.ok(null);
    }

    /**
     * Enable user (Admin only)
     */
    @PostMapping("/{userId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> enableUser(@PathVariable UUID userId) {
        UUID operatorId = getCurrentUserId();
        userService.updateUserStatus(userId, UserStatus.ENABLED, operatorId);
        
        return ResponseEntity.ok(null);
    }

    /**
     * Disable user (Admin only)
     */
    @PostMapping("/{userId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> disableUser(@PathVariable UUID userId) {
        UUID operatorId = getCurrentUserId();
        userService.updateUserStatus(userId, UserStatus.DISABLED, operatorId);
        
        return ResponseEntity.ok(null);
    }

    /**
     * Delete user (Admin only)
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        UUID operatorId = getCurrentUserId();
        userService.deleteUser(userId, operatorId);
        
        return ResponseEntity.ok(null);
    }

    /**
     * Reset user password (Admin only)
     */
    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetUserPassword(@PathVariable UUID userId) {
        UUID operatorId = getCurrentUserId();
        
        // Generate a random password
        String newPassword = generateRandomPassword();
        userService.resetPassword(userId, newPassword, operatorId);
        
        return ResponseEntity.ok(newPassword);
    }

    // Helper methods

    /**
     * Get current authenticated user ID
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Assuming the principal contains user ID or we can extract it
        // This might need adjustment based on your JWT implementation
        return UUID.fromString(authentication.getName());
    }

    /**
     * Check if current user has admin role
     */
    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Generate random password for password reset
     */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    /**
     * Convert User entity to UserResponse DTO
     */
    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getEmail(),
            user.getRole(),
            user.getStatus() == UserStatus.ENABLED,
            user.getCreatedAt()
        );
    }
}