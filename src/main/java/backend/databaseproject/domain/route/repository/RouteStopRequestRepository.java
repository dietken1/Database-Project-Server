package backend.databaseproject.domain.route.repository;

import backend.databaseproject.domain.route.entity.RouteStopRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 경로 정류장-배송요청 매핑 Repository
 */
@Repository
public interface RouteStopRequestRepository extends JpaRepository<RouteStopRequest, Long> {
}
