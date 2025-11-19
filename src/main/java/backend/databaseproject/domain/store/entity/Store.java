package backend.databaseproject.domain.store.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 드론배송 지원 매장 엔티티
 */
@Entity
@Table(name = "store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long storeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreType type;

    @Column(length = 30)
    private String phone;

    @Column(length = 200)
    private String address;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(name = "delivery_radius_km", nullable = false, precision = 5, scale = 2)
    private BigDecimal deliveryRadiusKm = BigDecimal.valueOf(2.00);

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
    }

    @Builder
    public Store(String name, StoreType type, String phone, String address,
                 BigDecimal lat, BigDecimal lng, BigDecimal deliveryRadiusKm, Boolean isActive) {
        this.name = name;
        this.type = type;
        this.phone = phone;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.deliveryRadiusKm = deliveryRadiusKm != null ? deliveryRadiusKm : BigDecimal.valueOf(2.00);
        this.isActive = isActive != null ? isActive : true;
    }
}
