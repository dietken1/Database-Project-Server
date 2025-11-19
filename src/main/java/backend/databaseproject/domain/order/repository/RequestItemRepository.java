package backend.databaseproject.domain.order.repository;

import backend.databaseproject.domain.order.entity.RequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 주문 항목 Repository
 */
@Repository
public interface RequestItemRepository extends JpaRepository<RequestItem, Long> {
}
