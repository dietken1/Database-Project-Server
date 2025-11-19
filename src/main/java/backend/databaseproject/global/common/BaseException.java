package backend.databaseproject.global.common;

import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 커스텀 예외
 * ErrorCode를 포함하여 일관된 예외 처리를 제공합니다.
 */
@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
