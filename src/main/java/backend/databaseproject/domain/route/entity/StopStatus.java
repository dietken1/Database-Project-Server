package backend.databaseproject.domain.route.entity;

/**
 * 정류장 상태 Enum
 */
public enum StopStatus {
    PENDING,   // 대기 중
    ARRIVED,   // 도착함
    DEPARTED,  // 출발함
    SKIPPED,   // 건너뜀
    FAILED     // 실패
}
