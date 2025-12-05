package backend.databaseproject.global.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 전역 에러 코드 정의
 * 각 도메인별로 에러 코드를 관리하며, HTTP 상태 코드와 메시지를 포함합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common Errors (1000번대)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "잘못된 타입입니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "C005", "필수 요청 파라미터가 누락되었습니다."),

    // Store Errors (2000번대)
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "존재하지 않는 매장입니다."),
    STORE_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "S002", "운영 중이지 않은 매장입니다."),
    STORE_OUT_OF_DELIVERY_RANGE(HttpStatus.BAD_REQUEST, "S003", "배송 가능 범위를 벗어났습니다."),

    // Product Errors (3000번대)
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "존재하지 않는 상품입니다."),
    PRODUCT_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "P002", "판매 중이지 않은 상품입니다."),
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "P003", "재고가 부족합니다."),
    PRODUCT_EXCEED_MAX_QUANTITY(HttpStatus.BAD_REQUEST, "P004", "최대 주문 수량을 초과했습니다."),

    // User Errors (4000번대)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 사용자입니다."),

    // Order Errors (5000번대)
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "존재하지 않는 주문입니다."),
    ORDER_ALREADY_ASSIGNED(HttpStatus.BAD_REQUEST, "O002", "이미 배송이 할당된 주문입니다."),
    ORDER_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "O003", "이미 취소된 주문입니다."),
    ORDER_ITEMS_EMPTY(HttpStatus.BAD_REQUEST, "O004", "주문 항목이 비어있습니다."),
    ORDER_TOTAL_WEIGHT_EXCEEDED(HttpStatus.BAD_REQUEST, "O005", "주문 총 무게가 드론 적재 한계를 초과했습니다."),
    ORDER_ALREADY_PROCESSED(HttpStatus.CONFLICT, "O006", "이미 처리된 주문입니다."),

    // Drone Errors (6000번대)
    DRONE_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "존재하지 않는 드론입니다."),
    DRONE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "D002", "사용 가능한 드론이 없습니다."),
    DRONE_IN_FLIGHT(HttpStatus.BAD_REQUEST, "D003", "비행 중인 드론입니다."),

    // Route Errors (7000번대)
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "존재하지 않는 배송 경로입니다."),
    ROUTE_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "R002", "이미 완료된 배송입니다."),
    ROUTE_OPTIMIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "R003", "경로 최적화에 실패했습니다."),
    NO_DELIVERY_REQUESTS(HttpStatus.BAD_REQUEST, "R004", "배송 요청이 없습니다."),
    POSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "R005", "드론 위치 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
