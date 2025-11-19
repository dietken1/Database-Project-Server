package backend.databaseproject.domain.route.repository;

import backend.databaseproject.domain.route.entity.RoutePosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 드론 위치 Repository
 */
@Repository
public interface RoutePositionRepository extends JpaRepository<RoutePosition, Long> {

    /**
     * 특정 경로의 최신 위치 조회
     */
    @Query("SELECT rp FROM RoutePosition rp " +
           "WHERE rp.route.routeId = :routeId " +
           "ORDER BY rp.ts DESC " +
           "LIMIT 1")
    Optional<RoutePosition> findLatestByRouteId(@Param("routeId") Long routeId);
}
