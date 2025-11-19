package backend.databaseproject.domain.route.repository;

import backend.databaseproject.domain.route.entity.FlightLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 비행 로그 Repository
 */
@Repository
public interface FlightLogRepository extends JpaRepository<FlightLog, Long> {

    /**
     * 특정 경로의 비행 로그 조회
     */
    List<FlightLog> findByRouteRouteId(Long routeId);

    /**
     * 특정 드론의 비행 로그 조회
     */
    List<FlightLog> findByDroneDroneId(Long droneId);
}
