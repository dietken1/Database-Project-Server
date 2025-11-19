package backend.databaseproject.domain.route.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 배송 경로 드론 위치 추적 엔티티
 */
@Entity
@Table(name = "route_position")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutePosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Long positionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(name = "altitude_m", precision = 8, scale = 2)
    private BigDecimal altitudeM;

    @Column(name = "speed_kmh", precision = 6, scale = 2)
    private BigDecimal speedKmh;

    @Column(nullable = false)
    private Integer battery;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }

    @Builder
    public RoutePosition(Route route, BigDecimal lat, BigDecimal lng, BigDecimal altitudeM,
                         BigDecimal speedKmh, Integer battery, LocalDateTime recordedAt) {
        this.route = route;
        this.lat = lat;
        this.lng = lng;
        this.altitudeM = altitudeM;
        this.speedKmh = speedKmh;
        this.battery = battery;
        this.recordedAt = recordedAt;
    }

    /**
     * 경로 설정 (양방향 관계용)
     */
    protected void setRoute(Route route) {
        this.route = route;
    }
}
