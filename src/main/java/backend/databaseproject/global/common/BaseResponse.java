package backend.databaseproject.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 응답 공통 포맷
 * 모든 API 응답은 이 형식을 따릅니다.
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    private boolean success;
    private T data;
    private ErrorResponse error;

    /**
     * 성공 응답 (데이터 포함)
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(true, data, null);
    }

    /**
     * 성공 응답 (데이터 없음)
     */
    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>(true, null, null);
    }

    /**
     * 실패 응답
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(false, null, new ErrorResponse(errorCode));
    }

    /**
     * 실패 응답 (커스텀 메시지)
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(false, null, new ErrorResponse(errorCode, message));
    }

    /**
     * 에러 응답 상세 정보
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;

        public ErrorResponse(ErrorCode errorCode) {
            this.code = errorCode.getCode();
            this.message = errorCode.getMessage();
        }

        public ErrorResponse(ErrorCode errorCode, String message) {
            this.code = errorCode.getCode();
            this.message = message;
        }
    }
}
