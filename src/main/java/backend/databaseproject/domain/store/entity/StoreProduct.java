package backend.databaseproject.domain.store.entity;

import backend.databaseproject.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 점포별 판매 상품 엔티티
 */
@Entity
@Table(name = "store_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(StoreProduct.StoreProductId.class)
public class StoreProduct {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "stock_qty", nullable = false)
    private Integer stockQty = 0;

    @Column(name = "max_qty_per_order", nullable = false)
    private Integer maxQtyPerOrder = 10;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public StoreProduct(Store store, Product product, Integer price,
                        Integer stockQty, Integer maxQtyPerOrder, Boolean isActive) {
        this.store = store;
        this.product = product;
        this.price = price;
        this.stockQty = stockQty != null ? stockQty : 0;
        this.maxQtyPerOrder = maxQtyPerOrder != null ? maxQtyPerOrder : 10;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * 재고 감소
     */
    public void decreaseStock(int quantity) {
        this.stockQty -= quantity;
    }

    /**
     * 복합 키 클래스
     */
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class StoreProductId implements Serializable {
        private Long store;
        private Long product;

        public StoreProductId(Long store, Long product) {
            this.store = store;
            this.product = product;
        }
    }
}
