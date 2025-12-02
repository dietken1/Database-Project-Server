package backend.databaseproject.domain.route.repository;

import backend.databaseproject.domain.route.entity.RouteStopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 경로 정류장-주문 매핑 Repository
 */
@Repository
public interface RouteStopOrderRepository extends JpaRepository<RouteStopOrder, Long> {

    /**
     * 주문 ID로 경로 ID 조회
     * 주문이 배송 경로에 할당된 경우 해당 경로 ID를 반환
     *
     * @param orderId 주문 ID
     * @return 경로 ID (Optional)
     */
    @Query("SELECT rso.routeStop.route.routeId FROM RouteStopOrder rso " +
           "WHERE rso.order.orderId = :orderId")
    Optional<Long> findRouteIdByOrderId(@Param("orderId") Long orderId);

    /**
     * 여러 주문 ID들의 경로 ID를 배치로 조회
     * N+1 문제 방지: IN 절을 사용한 배치 조회
     *
     * @param orderIds 주문 ID 리스트
     * @return 주문 ID와 경로 ID의 매핑 리스트 (Object[]: [orderId, routeId])
     */
    @Query("SELECT rso.order.orderId, rso.routeStop.route.routeId FROM RouteStopOrder rso " +
           "WHERE rso.order.orderId IN :orderIds")
    List<Object[]> findRouteIdsByOrderIds(@Param("orderIds") List<Long> orderIds);

    /**
     * 여러 주문 ID들의 경로 ID를 Map으로 조회하는 편의 메서드
     * N+1 문제 방지: 배치 조회 후 Map으로 변환
     *
     * @param orderIds 주문 ID 리스트
     * @return 주문 ID를 키로, 경로 ID를 값으로 하는 Map
     */
    default Map<Long, Long> findRouteIdsMapByOrderIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }

        return findRouteIdsByOrderIds(orderIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],  // orderId
                        row -> (Long) row[1]   // routeId
                ));
    }
}
