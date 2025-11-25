package backend.databaseproject.domain.drone.repository;

import backend.databaseproject.domain.drone.entity.Drone;
import backend.databaseproject.domain.drone.entity.DroneStatus;
import backend.databaseproject.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
     * @deprecated 매장별 드론 조회를 위해 findFirstByStoreAndStatus 사용 권장
     */
    @Deprecated
    Optional<Drone> findFirstByStatus(DroneStatus status);

    /**
     * 특정 매장의 대기 중인 드론 조회 (매장별 드론 할당)
     */
    Optional<Drone> findFirstByStoreAndStatus(Store store, DroneStatus status);

    /**
     * 대기 중인 드론 수 조회
     */
    Long countByStatus(DroneStatus status);

    /**
     * 특정 매장의 드론 목록 조회
     */
    List<Drone> findByStore(Store store);

    /**
     * 시스템 드론들의 최소 최대 적재 무게 조회
     * 주문 생성 시 무게 검증에 사용
     */
    @Query("SELECT MIN(d.maxPayloadKg) FROM Drone d")
    Optional<BigDecimal> findMinMaxPayloadKg();
}
