package backend.databaseproject.domain.drone.entity;

// 드론 상태 Enum
public enum DroneStatus {
    IDLE,         // 대기
    IN_FLIGHT,    // 비행 중
    CHARGING,     // 충전 중
    MAINTENANCE,  // 정비 중
    RETIRED       // 퇴역
}
