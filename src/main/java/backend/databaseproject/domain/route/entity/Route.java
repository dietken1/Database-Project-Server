package backend.databaseproject.domain.route.entity;

import backend.databaseproject.domain.drone.entity.Drone;
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
 * 배송 경로 엔티티
 */
@Entity
@Table(name = "route")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Long routeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drone_id", nullable = false)
    private Drone drone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "total_distance_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalDistanceKm;

    @Column(name = "total_weight_kg", nullable = false, precision = 8, scale = 3)
    private BigDecimal totalWeightKg;

    @Column(name = "estimated_duration_min", nullable = false)
    private Integer estimatedDurationMin;

    @Column(name = "actual_duration_min")
    private Integer actualDurationMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RouteStatus status = RouteStatus.PLANNED;

    @Column(name = "launched_at")
    private LocalDateTime launchedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteStop> routeStops = new ArrayList<>();

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoutePosition> routePositions = new ArrayList<>();

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlightLog> flightLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public Route(Drone drone, Store store, BigDecimal totalDistanceKm, BigDecimal totalWeightKg,
                 Integer estimatedDurationMin, String note) {
        this.drone = drone;
        this.store = store;
        this.totalDistanceKm = totalDistanceKm;
        this.totalWeightKg = totalWeightKg;
        this.estimatedDurationMin = estimatedDurationMin;
        this.status = RouteStatus.PLANNED;
        this.note = note;
    }

    /**
     * 경로 상태 변경
     */
    public void changeStatus(RouteStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 배송 시작
     */
    public void launch() {
        this.status = RouteStatus.LAUNCHED;
        this.launchedAt = LocalDateTime.now();
    }

    /**
     * 배송 완료
     */
    public void complete() {
        this.status = RouteStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 배송 중단
     */
    public void abort() {
        this.status = RouteStatus.ABORTED;
    }

    /**
     * 정류장 추가
     */
    public void addRouteStop(RouteStop routeStop) {
        this.routeStops.add(routeStop);
        routeStop.setRoute(this);
    }

    /**
     * 드론 위치 추가
     */
    public void addRoutePosition(RoutePosition routePosition) {
        this.routePositions.add(routePosition);
        routePosition.setRoute(this);
    }

    /**
     * 비행 로그 추가
     */
    public void addFlightLog(FlightLog flightLog) {
        this.flightLogs.add(flightLog);
        flightLog.setRoute(this);
    }
}
