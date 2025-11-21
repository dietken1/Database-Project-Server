package backend.databaseproject.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RESTful API 에러 응답 포맷
 * HTTP 상태 코드와 함께 에러 상세 정보를 제공합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String code;
    private String message;
    private LocalDateTime timestamp;

    public ErrorResponse(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(ErrorCode errorCode, String message) {
        this.code = errorCode.getCode();
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode, message);
    }
}
