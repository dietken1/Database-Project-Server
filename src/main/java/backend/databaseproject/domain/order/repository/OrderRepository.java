package backend.databaseproject.domain.order.repository;

import backend.databaseproject.domain.order.entity.Order;
import backend.databaseproject.domain.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 주문 Repository
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 상태별 주문 조회
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * 특정 매장의 특정 상태 주문 조회
     */
    List<Order> findByStoreStoreIdAndStatus(Long storeId, OrderStatus status);

    /**
     * 배송 대기 중인 주문들을 매장별로 그룹화하여 조회
     */
    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.store " +
           "JOIN FETCH o.user " +
           "WHERE o.status = :status " +
           "ORDER BY o.store.storeId, o.createdAt")
    List<Order> findPendingOrdersWithStoreAndUser(@Param("status") OrderStatus status);
}
