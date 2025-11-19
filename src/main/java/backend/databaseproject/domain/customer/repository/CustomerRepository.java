package backend.databaseproject.domain.customer.repository;

import backend.databaseproject.domain.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 고객 Repository
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
