package ai.synalix.synalixai.enums;

/**
 * Audit operation type enumeration
 */
public enum AuditOperationType {
    /**
     * User login
     */
    USER_LOGIN,
    
    /**
     * User logout
     */
    USER_LOGOUT,
    
    /**
     * Create user
     */
    USER_CREATE,
    
    /**
     * Update user information
     */
    USER_UPDATE,
    
    /**
     * Delete user
     */
    USER_DELETE,
    
    /**
     * Password change
     */
    PASSWORD_CHANGE,
    
    /**
     * Password reset
     */
    PASSWORD_RESET,
    
    /**
     * Role update
     */
    ROLE_UPDATE,
    
    /**
     * Status update
     */
    STATUS_UPDATE,
    
    /**
     * Token refresh
     */
    TOKEN_REFRESH,
    
    /**
     * Token revocation
     */
    TOKEN_REVOKE,

    /**
     * Create message
     */
    MESSAGE_CREATE,

    /**
     * Delete message
     */
    MESSAGE_DELETE
}