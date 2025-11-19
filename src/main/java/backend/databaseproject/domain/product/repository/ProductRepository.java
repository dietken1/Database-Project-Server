package backend.databaseproject.domain.product.repository;

import backend.databaseproject.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 상품 Repository
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 활성화된 상품만 조회
     */
    List<Product> findByIsActiveTrue();

    /**
     * 카테고리별 활성화된 상품 조회
     */
    List<Product> findByCategoryAndIsActiveTrue(String category);
}
