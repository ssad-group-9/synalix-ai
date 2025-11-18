package ai.synalix.synalixai.exception;

import ai.synalix.synalixai.enums.ApiErrorCode;

/**
 * Unified API exception class
 * All business exceptions should use this class with appropriate ApiErrorCode
 */
public class ApiException extends RuntimeException {

    private final ApiErrorCode errorCode;
    private final Object details;

    public ApiException(ApiErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public ApiException(ApiErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.details = null;
    }

    public ApiException(ApiErrorCode errorCode, Object details) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    public ApiException(ApiErrorCode errorCode, String customMessage, Object details) {
        super(customMessage);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ApiException(ApiErrorCode errorCode, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    public ApiException(ApiErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public Object getDetails() {
        return details;
    }
}