package backend.databaseproject.domain.order.entity;

import backend.databaseproject.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 주문 항목 엔티티
 */
@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "unit_weight_kg", nullable = false, precision = 6, scale = 3)
    private BigDecimal unitWeightKg;

    @Builder
    public OrderItem(Order order, Product product,
                     Integer quantity, Integer unitPrice, BigDecimal unitWeightKg) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.unitWeightKg = unitWeightKg;
    }

    /**
     * 양방향 연관관계 설정용
     */
    protected void setOrder(Order order) {
        this.order = order;
    }
}
