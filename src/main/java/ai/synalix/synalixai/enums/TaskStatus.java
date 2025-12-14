package ai.synalix.synalixai.enums;

/**
 * Task status enumeration
 */
public enum TaskStatus {
    /**
     * Task is pending execution
     */
    PENDING,

    /**
     * Task is currently running
     */
    RUNNING,

    /**
     * Task completed successfully
     */
    COMPLETED,

    /**
     * Task failed
     */
    FAILED,

    /**
     * Task was stopped by user
     */
    STOPPED
}
