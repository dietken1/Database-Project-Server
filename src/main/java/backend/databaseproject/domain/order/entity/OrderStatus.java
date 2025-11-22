package backend.databaseproject.domain.order.entity;

/**
 * 주문 상태 Enum
 */
public enum OrderStatus {
    CREATED,   // 주문 생성
    ASSIGNED,  // 배송 할당됨
    FULFILLED, // 배송 완료
    CANCELED,  // 취소됨
    FAILED     // 실패
}
