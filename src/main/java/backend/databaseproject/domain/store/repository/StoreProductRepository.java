package backend.databaseproject.domain.store.repository;

import backend.databaseproject.domain.store.entity.Store;
import backend.databaseproject.domain.store.entity.StoreProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 매장 상품 Repository
 */
@Repository
public interface StoreProductRepository extends JpaRepository<StoreProduct, StoreProduct.StoreProductId> {

    /**
     * 특정 매장의 활성화된 상품 목록 조회
     */
    @Query("SELECT sp FROM StoreProduct sp " +
           "JOIN FETCH sp.product p " +
           "WHERE sp.store.storeId = :storeId AND sp.isActive = true AND p.isActive = true")
    List<StoreProduct> findActiveProductsByStoreId(@Param("storeId") Long storeId);

    /**
     * 특정 매장의 특정 카테고리 상품 조회
     */
    @Query("SELECT sp FROM StoreProduct sp " +
           "JOIN FETCH sp.product p " +
           "WHERE sp.store.storeId = :storeId AND p.category = :category " +
           "AND sp.isActive = true AND p.isActive = true")
    List<StoreProduct> findActiveProductsByStoreIdAndCategory(
            @Param("storeId") Long storeId,
            @Param("category") String category);

    /**
     * 특정 매장의 카테고리 목록 조회
     */
    @Query("SELECT DISTINCT p.category FROM StoreProduct sp " +
           "JOIN sp.product p " +
           "WHERE sp.store.storeId = :storeId AND sp.isActive = true AND p.isActive = true " +
           "ORDER BY p.category")
    List<String> findCategoriesByStoreId(@Param("storeId") Long storeId);
}
