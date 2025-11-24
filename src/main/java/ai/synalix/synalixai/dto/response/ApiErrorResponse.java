package ai.synalix.synalixai.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Standardized API error response
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private final LocalDateTime timestamp;
    
    private int status;
    
    private String error;
    
    private String code;
    
    private String message;
    
    private String path;
    
    private Object details;

    public ApiErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiErrorResponse(int status, String error, String code, String message, String path) {
        this();
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
    }

    public ApiErrorResponse(int status, String error, String code, String message, String path, Object details) {
        this(status, error, code, message, path);
        this.details = details;
    }

}