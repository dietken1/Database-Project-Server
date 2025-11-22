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
    @Column(name = "type", nullable = false, length = 20)
    private StopType stopType;

    @Column(length = 120)
    private String name;

    @Column(precision = 9, scale = 6, nullable = false)
    private BigDecimal lat;

    @Column(precision = 9, scale = 6, nullable = false)
    private BigDecimal lng;

    @Column(name = "planned_arrival_at")
    private LocalDateTime plannedArrivalAt;

    @Column(name = "planned_departure_at")
    private LocalDateTime plannedDepartureAt;

    @Column(name = "actual_arrival_at")
    private LocalDateTime actualArrivalAt;

    @Column(name = "actual_departure_at")
    private LocalDateTime actualDepartureAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StopStatus status = StopStatus.PENDING;

    @Column(name = "payload_delta_kg", precision = 7, scale = 3)
    private BigDecimal payloadDeltaKg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "routeStop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteStopOrder> routeStopOrders = new ArrayList<>();

    @Builder
    public RouteStop(Route route, Integer stopSequence, StopType stopType, String name,
                     BigDecimal lat, BigDecimal lng,
                     LocalDateTime plannedArrivalAt, LocalDateTime plannedDepartureAt,
                     BigDecimal payloadDeltaKg,
                     Store store, Customer customer, String note) {
        this.route = route;
        this.stopSequence = stopSequence;
        this.stopType = stopType;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.plannedArrivalAt = plannedArrivalAt;
        this.plannedDepartureAt = plannedDepartureAt;
        this.payloadDeltaKg = payloadDeltaKg;
        this.store = store;
        this.customer = customer;
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
        this.actualArrivalAt = LocalDateTime.now();
    }

    /**
     * 출발 표시
     */
    public void depart() {
        this.status = StopStatus.DEPARTED;
        this.actualDepartureAt = LocalDateTime.now();
    }

    /**
     * 건너뜀 표시
     */
    public void skip() {
        this.status = StopStatus.SKIPPED;
    }

    /**
     * 주문 추가
     */
    public void addRouteStopOrder(RouteStopOrder routeStopOrder) {
        this.routeStopOrders.add(routeStopOrder);
        routeStopOrder.setRouteStop(this);
    }
}
