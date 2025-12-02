package backend.databaseproject.domain.order.repository;

import backend.databaseproject.domain.order.entity.Order;
import backend.databaseproject.domain.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
     * N+1 문제 방지: JOIN FETCH로 Store, User를 함께 조회
     */
    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.store " +
           "JOIN FETCH o.user " +
           "WHERE o.status = :status " +
           "ORDER BY o.store.storeId, o.createdAt")
    List<Order> findPendingOrdersWithStoreAndUser(@Param("status") OrderStatus status);

    /**
     * 특정 매장의 모든 주문 조회 (Store, User, OrderItems, Product를 함께 조회)
     * N+1 문제 방지: JOIN FETCH로 연관 엔티티들을 함께 조회
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN FETCH o.store " +
           "JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product " +
           "WHERE o.store.storeId = :storeId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByStoreIdWithDetails(@Param("storeId") Long storeId);

    /**
     * 특정 매장의 특정 상태 주문 조회 (Store, User, OrderItems, Product를 함께 조회)
     * N+1 문제 방지: JOIN FETCH로 연관 엔티티들을 함께 조회
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN FETCH o.store " +
           "JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product " +
           "WHERE o.store.storeId = :storeId " +
           "AND o.status = :status " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByStoreIdAndStatusWithDetails(
            @Param("storeId") Long storeId,
            @Param("status") OrderStatus status);

    /**
     * 주문 ID로 상세 조회 (OrderItems, Product를 함께 조회)
     * N+1 문제 방지: JOIN FETCH로 연관 엔티티들을 함께 조회
     */
    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.store " +
           "JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product " +
           "WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);
}
