package backend.databaseproject.domain.order.entity;

import backend.databaseproject.domain.user.entity.User;
import backend.databaseproject.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 엔티티
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "origin_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal originLat;

    @Column(name = "origin_lng", nullable = false, precision = 9, scale = 6)
    private BigDecimal originLng;

    @Column(name = "dest_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal destLat;

    @Column(name = "dest_lng", nullable = false, precision = 9, scale = 6)
    private BigDecimal destLng;

    @Column(name = "total_weight_kg", nullable = false, precision = 8, scale = 3)
    private BigDecimal totalWeightKg;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public Order(Store store, User user,
                 BigDecimal originLat, BigDecimal originLng,
                 BigDecimal destLat, BigDecimal destLng,
                 BigDecimal totalWeightKg, Integer totalAmount, Integer itemCount,
                 String note) {
        this.store = store;
        this.user = user;
        this.originLat = originLat;
        this.originLng = originLng;
        this.destLat = destLat;
        this.destLng = destLng;
        this.totalWeightKg = totalWeightKg;
        this.totalAmount = totalAmount;
        this.itemCount = itemCount;
        this.status = OrderStatus.CREATED;
        this.note = note;
    }

    /**
     * 주문 항목 추가
     */
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    /**
     * 배송 할당
     */
    public void assignDelivery() {
        this.status = OrderStatus.ASSIGNED;
        this.assignedAt = LocalDateTime.now();
    }

    /**
     * 배송 완료
     */
    public void completeDelivery() {
        this.status = OrderStatus.FULFILLED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 주문 취소
     */
    public void cancel() {
        this.status = OrderStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    /**
     * 배송 실패
     */
    public void fail(String reason) {
        this.status = OrderStatus.FAILED;
        this.failureReason = reason;
    }
}
