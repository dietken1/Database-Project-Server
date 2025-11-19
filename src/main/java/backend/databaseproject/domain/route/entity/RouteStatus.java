package backend.databaseproject.domain.route.entity;

/**
 * 배송 경로 상태 Enum
 */
public enum RouteStatus {
    PLANNED,      // 계획됨
    LAUNCHED,     // 출발함
    IN_PROGRESS,  // 진행 중
    COMPLETED,    // 완료
    ABORTED       // 중단됨
}
