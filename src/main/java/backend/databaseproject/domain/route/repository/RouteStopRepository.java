package backend.databaseproject.domain.route.repository;

import backend.databaseproject.domain.route.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 경로 정류장 Repository
 */
@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    /**
     * 특정 경로의 정류장 목록 조회 (순서대로)
     */
    List<RouteStop> findByRouteRouteIdOrderByStopSeq(Long routeId);

    /**
     * 특정 경로의 다음 정류장 조회 (PENDING 상태)
     */
    @Query("SELECT rs FROM RouteStop rs " +
           "WHERE rs.route.routeId = :routeId AND rs.status = 'PENDING' " +
           "ORDER BY rs.stopSeq ASC " +
           "LIMIT 1")
    RouteStop findNextPendingStop(@Param("routeId") Long routeId);
}
