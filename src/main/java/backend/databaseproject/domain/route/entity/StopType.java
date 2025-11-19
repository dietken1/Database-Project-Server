package backend.databaseproject.domain.route.entity;

/**
 * 정류장 타입 Enum
 */
public enum StopType {
    PICKUP,  // 픽업 (매장에서 물건 싣기)
    DROP,    // 드롭 (고객에게 배송)
    RETURN   // 복귀 (매장으로 돌아오기)
}
