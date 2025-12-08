package ai.synalix.synalixai.enums;

/**
 * Dataset status enumeration
 * Defines the lifecycle states of a dataset
 */
public enum DatasetStatus {
    
    /**
     * Dataset metadata created, awaiting file upload
     */
    PENDING_UPLOAD,
    
    /**
     * File upload in progress
     */
    UPLOADING,
    
    /**
     * File uploaded successfully, ready for use
     */
    READY,
    
    /**
     * Dataset is being processed (e.g., train/test split)
     */
    PROCESSING,
    
    /**
     * Dataset processing failed
     */
    FAILED,
    
    /**
     * Dataset has been deleted (soft delete)
     */
    DELETED
}