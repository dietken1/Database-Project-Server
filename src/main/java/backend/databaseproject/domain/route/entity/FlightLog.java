package backend.databaseproject.domain.route.entity;

import backend.databaseproject.domain.drone.entity.Drone;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 비행 로그 엔티티
 */
@Entity
@Table(name = "flight_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlightLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flight_log_id")
    private Long flightLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drone_id", nullable = false)
    private Drone drone;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "distance_km", precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(name = "max_altitude_m", precision = 8, scale = 2)
    private BigDecimal maxAltitudeM;

    @Column(name = "avg_speed_kmh", precision = 6, scale = 2)
    private BigDecimal avgSpeedKmh;

    @Column(name = "battery_used")
    private Integer batteryUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlightResult result;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public FlightLog(Route route, Drone drone, LocalDateTime startTime, LocalDateTime endTime,
                     BigDecimal distanceKm, Integer durationMin, BigDecimal maxAltitudeM,
                     BigDecimal avgSpeedKmh, Integer batteryUsed, FlightResult result,
                     String errorMessage) {
        this.route = route;
        this.drone = drone;
        this.startTime = startTime;
        this.endTime = endTime;
        this.distanceKm = distanceKm;
        this.durationMin = durationMin;
        this.maxAltitudeM = maxAltitudeM;
        this.avgSpeedKmh = avgSpeedKmh;
        this.batteryUsed = batteryUsed;
        this.result = result;
        this.errorMessage = errorMessage;
    }

    /**
     * 경로 설정 (양방향 관계용)
     */
    protected void setRoute(Route route) {
        this.route = route;
    }
}
