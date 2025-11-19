package backend.databaseproject.domain.order.repository;

import backend.databaseproject.domain.order.entity.DeliveryRequest;
import backend.databaseproject.domain.order.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 배송 요청(주문) Repository
 */
@Repository
public interface DeliveryRequestRepository extends JpaRepository<DeliveryRequest, Long> {

    /**
     * 상태별 배송 요청 조회
     */
    List<DeliveryRequest> findByStatus(DeliveryStatus status);

    /**
     * 특정 매장의 특정 상태 배송 요청 조회
     */
    List<DeliveryRequest> findByStoreStoreIdAndStatus(Long storeId, DeliveryStatus status);

    /**
     * 배송 대기 중인 요청들을 매장별로 그룹화하여 조회
     */
    @Query("SELECT dr FROM DeliveryRequest dr " +
           "JOIN FETCH dr.store " +
           "JOIN FETCH dr.customer " +
           "WHERE dr.status = :status " +
           "ORDER BY dr.store.storeId, dr.createdAt")
    List<DeliveryRequest> findPendingRequestsWithStoreAndCustomer(@Param("status") DeliveryStatus status);
}
