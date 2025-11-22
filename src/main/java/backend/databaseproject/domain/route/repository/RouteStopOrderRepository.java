package backend.databaseproject.domain.route.repository;

import backend.databaseproject.domain.route.entity.RouteStopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
}
