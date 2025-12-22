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
     * Create dataset metadata
     */
    DATASET_CREATE,
    
    /**
     * Update dataset information
     */
    DATASET_UPDATE,
    
    /**
     * Delete dataset
     */
    DATASET_DELETE,
    
    /**
     * Generate presigned URL for dataset upload
     */
    DATASET_UPLOAD_URL_GENERATED,
    
    /**
     * Dataset upload completed
     */
    DATASET_UPLOAD_COMPLETED,

    /**
     * Generate presigned URL for dataset download
     */
    DATASET_DOWNLOAD_URL_GENERATED,
    
    /**
     * Dataset preprocessing (train/test/eval split)
     */
    DATASET_PREPROCESS,

    /**
     * Register base model (admin)
     */
    MODEL_CREATE,
    
    /**
     * Update model information
     */
    MODEL_UPDATE,
    
    /**
     * Delete model (admin)
     */
    MODEL_DELETE,
    
    /**
     * Generate presigned URL for checkpoint download
     */
    CHECKPOINT_DOWNLOAD_URL_GENERATED,
    
    /**
     * Checkpoint upload completed (from training service)
     */
    CHECKPOINT_UPLOAD_COMPLETED
     * Create message
     */
    MESSAGE_CREATE,

    /**
     * Delete message
     */
    MESSAGE_DELETE
}