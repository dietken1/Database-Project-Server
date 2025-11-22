package backend.databaseproject.domain.order.repository;

import backend.databaseproject.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 주문 항목 Repository
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
