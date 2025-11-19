package backend.databaseproject.domain.route.entity;

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
 * 배송 경로 정류장 엔티티
 */
@Entity
@Table(name = "route_stop")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stop_id")
    private Long stopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StopType stopType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "arrival_lat", precision = 9, scale = 6)
    private BigDecimal arrivalLat;

    @Column(name = "arrival_lng", precision = 9, scale = 6)
    private BigDecimal arrivalLng;

    @Column(name = "distance_from_prev_km", precision = 8, scale = 2)
    private BigDecimal distanceFromPrevKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StopStatus status = StopStatus.PENDING;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "departed_at")
    private LocalDateTime departedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "routeStop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteStopRequest> routeStopRequests = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public RouteStop(Route route, Integer stopSequence, StopType stopType,
                     Store store, Customer customer, BigDecimal arrivalLat, BigDecimal arrivalLng,
                     BigDecimal distanceFromPrevKm, String note) {
        this.route = route;
        this.stopSequence = stopSequence;
        this.stopType = stopType;
        this.store = store;
        this.customer = customer;
        this.arrivalLat = arrivalLat;
        this.arrivalLng = arrivalLng;
        this.distanceFromPrevKm = distanceFromPrevKm;
        this.status = StopStatus.PENDING;
        this.note = note;
    }

    /**
     * 경로 설정 (양방향 관계용)
     */
    protected void setRoute(Route route) {
        this.route = route;
    }

    /**
     * 정류장 상태 변경
     */
    public void changeStatus(StopStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 도착 표시
     */
    public void arrive() {
        this.status = StopStatus.ARRIVED;
        this.arrivedAt = LocalDateTime.now();
    }

    /**
     * 출발 표시
     */
    public void depart() {
        this.status = StopStatus.DEPARTED;
        this.departedAt = LocalDateTime.now();
    }

    /**
     * 건너뜀 표시
     */
    public void skip() {
        this.status = StopStatus.SKIPPED;
    }

    /**
     * 배송 요청 추가
     */
    public void addRouteStopRequest(RouteStopRequest routeStopRequest) {
        this.routeStopRequests.add(routeStopRequest);
        routeStopRequest.setRouteStop(this);
    }
}
