package backend.databaseproject.domain.store.repository;

import backend.databaseproject.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 매장 Repository
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    /**
     * 활성화된 매장만 조회
     */
    List<Store> findByIsActiveTrue();

    /**
     * 특정 반경 내의 활성화된 매장 조회
     * Haversine 공식을 사용한 거리 계산
     */
    @Query(value = """
        SELECT s.* FROM store s
        WHERE s.is_active = 1
        AND (6371 * acos(
            cos(radians(:lat)) * cos(radians(s.lat)) *
            cos(radians(s.lng) - radians(:lng)) +
            sin(radians(:lat)) * sin(radians(s.lat))
        )) <= :radiusKm
        ORDER BY (6371 * acos(
            cos(radians(:lat)) * cos(radians(s.lat)) *
            cos(radians(s.lng) - radians(:lng)) +
            sin(radians(:lat)) * sin(radians(s.lat))
        ))
        """, nativeQuery = true)
    List<Store> findStoresWithinRadius(@Param("lat") BigDecimal lat,
                                       @Param("lng") BigDecimal lng,
                                       @Param("radiusKm") BigDecimal radiusKm);
}
