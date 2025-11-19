package backend.databaseproject.domain.drone.repository;

import backend.databaseproject.domain.drone.entity.Drone;
import backend.databaseproject.domain.drone.entity.DroneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 드론 Repository
 */
@Repository
public interface DroneRepository extends JpaRepository<Drone, Long> {

    /**
     * 상태별 드론 조회
     */
    List<Drone> findByStatus(DroneStatus status);

    /**
     * 첫 번째 대기 중인 드론 조회
     */
    Optional<Drone> findFirstByStatus(DroneStatus status);

    /**
     * 대기 중인 드론 수 조회
     */
    Long countByStatus(DroneStatus status);
}
