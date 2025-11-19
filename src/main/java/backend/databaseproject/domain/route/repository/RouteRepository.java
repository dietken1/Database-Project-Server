package backend.databaseproject.domain.route.repository;

import backend.databaseproject.domain.route.entity.Route;
import backend.databaseproject.domain.route.entity.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 배송 경로 Repository
 */
@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    /**
     * 상태별 경로 조회
     */
    List<Route> findByStatus(RouteStatus status);

    /**
     * 드론 ID와 상태로 경로 조회
     */
    List<Route> findByDroneDroneIdAndStatus(Long droneId, RouteStatus status);

    /**
     * 진행 중인 경로 조회 (RouteStop과 함께)
     */
    @Query("SELECT r FROM Route r " +
           "LEFT JOIN FETCH r.routeStops " +
           "WHERE r.status IN ('LAUNCHED', 'IN_PROGRESS') " +
           "ORDER BY r.actualStartAt DESC")
    List<Route> findActiveRoutesWithStops();

    /**
     * 특정 경로의 상세 정보 조회 (모든 연관 엔티티 fetch)
     */
    @Query("SELECT r FROM Route r " +
           "LEFT JOIN FETCH r.routeStops rs " +
           "LEFT JOIN FETCH r.drone " +
           "LEFT JOIN FETCH r.store " +
           "WHERE r.routeId = :routeId")
    Optional<Route> findByIdWithDetails(@Param("routeId") Long routeId);
}
