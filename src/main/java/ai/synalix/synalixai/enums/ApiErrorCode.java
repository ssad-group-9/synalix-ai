package ai.synalix.synalixai.enums;

import org.springframework.http.HttpStatus;

/**
 * API error code enumeration
 * Defines all possible error types with corresponding HTTP status codes and messages
 */
public enum ApiErrorCode {
    
    // Authentication & Authorization Errors (4xx)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Account is disabled"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token has expired"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid refresh token"),
    
    // Validation Errors (4xx)
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
    INVALID_REQUEST_FORMAT(HttpStatus.BAD_REQUEST, "Invalid request format"),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "Missing required field"),
    
    // Business Logic Errors (4xx)
    USERNAME_EXISTS(HttpStatus.CONFLICT, "Username already exists"),
    EMAIL_EXISTS(HttpStatus.CONFLICT, "Email already exists"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    INVALID_CURRENT_PASSWORD(HttpStatus.FORBIDDEN, "Current password is incorrect"),
    CANNOT_MODIFY_SELF(HttpStatus.FORBIDDEN, "Cannot modify your own account"),
    CANNOT_DELETE_SELF(HttpStatus.FORBIDDEN, "Cannot delete your own account"),
    CANNOT_DISABLE_SELF(HttpStatus.FORBIDDEN, "Cannot disable your own account"),
    CANNOT_DELETE_ADMIN(HttpStatus.FORBIDDEN, "Cannot delete administrator accounts"),
    
    // System Errors (5xx)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ApiErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public int getStatusCode() {
        return httpStatus.value();
    }
}