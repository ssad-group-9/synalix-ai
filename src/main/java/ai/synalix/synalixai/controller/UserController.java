package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.config.JwtAuthenticationFilter;
import ai.synalix.synalixai.config.JwtUserPrincipal;
import ai.synalix.synalixai.dto.user.*;
import ai.synalix.synalixai.entity.User;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.enums.UserRole;
import ai.synalix.synalixai.enums.UserStatus;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<CreateUserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request,
        @AuthenticationPrincipal JwtUserPrincipal principal) {
        var operatorId = principal.getId();

        // Generate random password
        var password = generateRandomPassword();

        var createdUser = userService.createUser(
            request.getUsername(),
            password,
            request.getNickname(),
            request.getEmail(),
            request.getRole(),
            operatorId
        );

        var userResponse = convertToUserResponse(createdUser);
        var response = new CreateUserResponse(userResponse, password);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all users (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        var users = userService.getAllUsers();
        var responses = users.stream()
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
        var user = userService.getUserById(userId);
        var response = convertToUserResponse(user);

        return ResponseEntity.ok(response);
    }

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile(@AuthenticationPrincipal JwtUserPrincipal principal) {
        var currentUserId = principal.getId();
        var user = userService.getUserById(currentUserId);
        var response = convertToUserResponse(user);

        return ResponseEntity.ok(response);
    }

    /**
     * Update user information
     */
    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        var operatorId = principal.getId();

        if (request.getEnabled() != null) {
            if (principal.getRole() == UserRole.ADMIN) {
                userService.updateUserStatus(userId, request.getEnabled() ? UserStatus.ENABLED : UserStatus.DISABLED, operatorId);
            } else {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED);
            }
        }

        if (request.getRole() != null) {
            if (principal.getRole() == UserRole.ADMIN) {
                userService.updateUserRole(userId, request.getRole(), operatorId);
            } else {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED);
            }
        }

        // Update basic information (nickname, email)
        var updatedUser = userService.updateUserInfo(
            userId,
            request.getNickname(),
            request.getEmail(),
            operatorId
        );

        var response = convertToUserResponse(updatedUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Update current user profile
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUserProfile(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {

        var currentUserId = principal.getId();

        if (request.getEnabled() != null) {
            if (principal.getRole() == UserRole.ADMIN) {
                userService.updateUserStatus(currentUserId, request.getEnabled() ? UserStatus.ENABLED : UserStatus.DISABLED, currentUserId);
            } else {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED);
            }
        }

        if (request.getRole() != null) {
            if (principal.getRole() == UserRole.ADMIN) {
                userService.updateUserRole(currentUserId, request.getRole(), currentUserId);
            } else {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED);
            }
        }

        // Regular users can only update nickname and email
        var updatedUser = userService.updateUserInfo(
            currentUserId,
            request.getNickname(),
            request.getEmail(),
            currentUserId
        );

        var response = convertToUserResponse(updatedUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Change current user password
     */
    @PostMapping("/me/password")
    public ResponseEntity<Void> changeCurrentUserPassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        var currentUserId = principal.getId();

        userService.changePassword(
            currentUserId,
            request.getOldPassword(),
            request.getNewPassword()
        );

        return ResponseEntity.ok(null);
    }

    /**
     * Delete user (Admin only)
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var operatorId = principal.getId();
        userService.deleteUser(userId, operatorId);

        return ResponseEntity.ok(null);
    }

    /**
     * Reset user password (Admin only)
     */
    @PostMapping("/{userId}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetUserPassword(
            @PathVariable UUID userId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var operatorId = principal.getId();

        // Generate a random password
        var newPassword = generateRandomPassword();
        userService.resetPassword(userId, newPassword, operatorId);

        return ResponseEntity.ok(newPassword);
    }

    /**
     * Generate random password for password reset
     */
    private String generateRandomPassword() {
        var chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        var password = new StringBuilder();
        var random = new java.util.Random();
        
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