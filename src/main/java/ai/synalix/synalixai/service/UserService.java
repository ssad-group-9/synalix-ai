package ai.synalix.synalixai.service;

import ai.synalix.synalixai.entity.User;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.enums.AuditOperationType;
import ai.synalix.synalixai.enums.UserRole;
import ai.synalix.synalixai.enums.UserStatus;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.RefreshTokenRepository;
import ai.synalix.synalixai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * User management service
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Autowired
    public UserService(UserRepository userRepository,
                      RefreshTokenRepository refreshTokenRepository,
                      PasswordEncoder passwordEncoder,
                      AuditService auditService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
     * Create new user (admin only)
     */
    @Transactional
    public User createUser(String username, String password, String nickname, String email, 
                          UserRole role, UUID operatorId) {
        


        // Check if username exists
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(ApiErrorCode.USERNAME_EXISTS);
        }

        // Check if email exists (if provided)
        if (email != null && !email.trim().isEmpty() && userRepository.existsByEmail(email)) {
            throw new ApiException(ApiErrorCode.EMAIL_EXISTS);
        }

        // Create user
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setRole(role != null ? role : UserRole.USER);
        user.setStatus(UserStatus.ENABLED);

        User savedUser = userRepository.save(user);

        // Audit log
        Map<String, Object> changes = Map.of(
                "username", username,
                "nickname", nickname,
                "email", email != null ? email : "",
                "role", savedUser.getRole().name(),
                "status", savedUser.getStatus().name()
        );
        auditService.logUserManagement(AuditOperationType.USER_CREATE, operatorId, savedUser.getId(), changes);

        logger.info("User created successfully: id={}, username={}, role={}", 
                   savedUser.getId(), savedUser.getUsername(), savedUser.getRole());

        return savedUser;
    }

    /**
     * Get user by ID
     */
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
    }

    /**
     * Get all users (admin only)
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Update user information
     */
    @Transactional
    public User updateUserInfo(UUID userId, String nickname, String email, UUID operatorId) {
        User user = getUserById(userId);

        Map<String, Object> changes = new HashMap<>();
        
        // Update nickname if provided
        if (nickname != null && !nickname.trim().isEmpty() && !nickname.equals(user.getNickname())) {
            String oldNickname = user.getNickname();
            user.setNickname(nickname);
            changes.put("nickname", Map.of("old", oldNickname, "new", nickname));
        }

        // Update email if provided
        if (email != null && !email.equals(user.getEmail())) {
            // Check if new email already exists
            if (!email.trim().isEmpty() && userRepository.existsByEmail(email)) {
                throw new ApiException(ApiErrorCode.EMAIL_EXISTS);
            }
            String oldEmail = user.getEmail();
            user.setEmail(email.trim().isEmpty() ? null : email);
            changes.put("email", Map.of("old", oldEmail != null ? oldEmail : "", "new", email));
        }

        if (!changes.isEmpty()) {
            User savedUser = userRepository.save(user);
            
            // Audit log
            auditService.logUserManagement(AuditOperationType.USER_UPDATE, operatorId, userId, changes);
            
            logger.info("User information updated: id={}, changes={}", userId, changes.keySet());
            return savedUser;
        }

        return user;
    }

    /**
     * Change user password
     */
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            auditService.logPasswordEvent(AuditOperationType.PASSWORD_CHANGE, userId, user.getUsername(), false);
            throw new ApiException(ApiErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all refresh tokens to force re-login
        int revokedCount = refreshTokenRepository.revokeAllTokensByUserId(userId);
        logger.info("Revoked {} refresh tokens for user: {}", revokedCount, userId);

        // Audit log
        auditService.logPasswordEvent(AuditOperationType.PASSWORD_CHANGE, userId, user.getUsername(), true);

        logger.info("Password changed successfully for user: id={}", userId);
    }

    /**
     * Reset user password (admin only)
     */
    @Transactional
    public void resetPassword(UUID userId, String newPassword, UUID operatorId) {
        User user = getUserById(userId);

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all refresh tokens to force re-login
        int revokedCount = refreshTokenRepository.revokeAllTokensByUserId(userId);
        logger.info("Revoked {} refresh tokens for user: {}", revokedCount, userId);

        // Audit log
        auditService.logPasswordEvent(AuditOperationType.PASSWORD_RESET, operatorId, user.getUsername(), true);

        logger.info("Password reset successfully for user: id={}, by operator: {}", userId, operatorId);
    }

    /**
     * Update user role (admin only)
     */
    @Transactional
    public User updateUserRole(UUID userId, UserRole newRole, UUID operatorId) {
        User user = getUserById(userId);
        User operator = getUserById(operatorId);

        // Only admin can change roles
        if (!operator.isAdmin()) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED);
        }

        // Prevent admin from changing their own role
        if (userId.equals(operatorId)) {
            throw new ApiException(ApiErrorCode.CANNOT_MODIFY_SELF);
        }

        // Check if there's a change
        if (user.getRole().equals(newRole)) {
            return user;
        }

        UserRole oldRole = user.getRole();
        user.setRole(newRole);
        User savedUser = userRepository.save(user);

        // Revoke all refresh tokens to force re-login with new role
        int revokedCount = refreshTokenRepository.revokeAllTokensByUserId(userId);
        logger.info("Revoked {} refresh tokens for user: {}", revokedCount, userId);

        // Audit log
        Map<String, Object> changes = Map.of(
                "role", Map.of("old", oldRole.name(), "new", newRole.name())
        );
        auditService.logUserManagement(AuditOperationType.ROLE_UPDATE, operatorId, userId, changes);

        logger.info("User role updated: id={}, oldRole={}, newRole={}, by operator: {}", 
                   userId, oldRole, newRole, operatorId);

        return savedUser;
    }

    /**
     * Update user status (admin only)
     */
    @Transactional
    public User updateUserStatus(UUID userId, UserStatus newStatus, UUID operatorId) {
        User user = getUserById(userId);

        // Prevent admin from disabling themselves
        if (userId.equals(operatorId) && newStatus == UserStatus.DISABLED) {
            throw new ApiException(ApiErrorCode.CANNOT_DISABLE_SELF);
        }

        // Check if there's a change
        if (user.getStatus().equals(newStatus)) {
            return user;
        }

        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        User savedUser = userRepository.save(user);

        // If disabling user, revoke all their tokens
        if (newStatus == UserStatus.DISABLED) {
            int revokedCount = refreshTokenRepository.revokeAllTokensByUserId(userId);
            logger.info("Revoked {} refresh tokens for disabled user: {}", revokedCount, userId);
        }

        // Audit log
        Map<String, Object> changes = Map.of(
                "status", Map.of("old", oldStatus.name(), "new", newStatus.name())
        );
        auditService.logUserManagement(AuditOperationType.STATUS_UPDATE, operatorId, userId, changes);

        logger.info("User status updated: id={}, oldStatus={}, newStatus={}, by operator: {}", 
                   userId, oldStatus, newStatus, operatorId);

        return savedUser;
    }

    /**
     * Delete user (admin only)
     */
    @Transactional
    public void deleteUser(UUID userId, UUID operatorId) {
        User user = getUserById(userId);

        // Prevent admin from deleting themselves
        if (userId.equals(operatorId)) {
            throw new ApiException(ApiErrorCode.CANNOT_DELETE_SELF);
        }

        // Prevent deleting other admins
        if (user.isAdmin()) {
            throw new ApiException(ApiErrorCode.CANNOT_DELETE_ADMIN);
        }

        // Revoke all refresh tokens
        int revokedCount = refreshTokenRepository.revokeAllTokensByUserId(userId);
        logger.info("Revoked {} refresh tokens for deleted user: {}", revokedCount, userId);

        // Delete user
        userRepository.delete(user);

        // Audit log
        Map<String, Object> eventData = Map.of(
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "role", user.getRole().name()
        );
        auditService.logUserManagement(AuditOperationType.USER_DELETE, operatorId, userId, eventData);

        logger.info("User deleted successfully: id={}, username={}, by operator: {}", 
                   userId, user.getUsername(), operatorId);
    }


}