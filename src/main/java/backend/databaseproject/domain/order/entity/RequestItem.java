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
@Table(name = "request_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_item_id")
    private Long requestItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private DeliveryRequest deliveryRequest;

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
    public RequestItem(DeliveryRequest deliveryRequest, Product product,
                       Integer quantity, Integer unitPrice, BigDecimal unitWeightKg) {
        this.deliveryRequest = deliveryRequest;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.unitWeightKg = unitWeightKg;
    }

    /**
     * 양방향 연관관계 설정용
     */
    protected void setDeliveryRequest(DeliveryRequest deliveryRequest) {
        this.deliveryRequest = deliveryRequest;
    }
}
