package backend.databaseproject.domain.order.entity;

import backend.databaseproject.domain.customer.entity.Customer;
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
 * 배송 요청(주문) 엔티티
 */
@Entity
@Table(name = "delivery_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

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
    private DeliveryStatus status = DeliveryStatus.CREATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "deliveryRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestItem> requestItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public DeliveryRequest(Store store, Customer customer,
                           BigDecimal originLat, BigDecimal originLng,
                           BigDecimal destLat, BigDecimal destLng,
                           BigDecimal totalWeightKg, Integer totalAmount, Integer itemCount,
                           String note) {
        this.store = store;
        this.customer = customer;
        this.originLat = originLat;
        this.originLng = originLng;
        this.destLat = destLat;
        this.destLng = destLng;
        this.totalWeightKg = totalWeightKg;
        this.totalAmount = totalAmount;
        this.itemCount = itemCount;
        this.status = DeliveryStatus.CREATED;
        this.note = note;
    }

    /**
     * 주문 항목 추가
     */
    public void addRequestItem(RequestItem requestItem) {
        this.requestItems.add(requestItem);
        requestItem.setDeliveryRequest(this);
    }

    /**
     * 배송 할당
     */
    public void assignDelivery() {
        this.status = DeliveryStatus.ASSIGNED;
        this.assignedAt = LocalDateTime.now();
    }

    /**
     * 배송 완료
     */
    public void completeDelivery() {
        this.status = DeliveryStatus.FULFILLED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 주문 취소
     */
    public void cancel() {
        this.status = DeliveryStatus.CANCELED;
    }
}
