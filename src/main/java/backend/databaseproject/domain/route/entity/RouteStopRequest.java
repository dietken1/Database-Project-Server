package backend.databaseproject.domain.route.entity;

import backend.databaseproject.domain.order.entity.DeliveryRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 배송 경로 정류장 - 배송 요청 매핑 엔티티
 */
@Entity
@Table(name = "route_stop_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteStopRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_stop_request_id")
    private Long routeStopRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private RouteStop routeStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private DeliveryRequest deliveryRequest;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public RouteStopRequest(RouteStop routeStop, DeliveryRequest deliveryRequest) {
        this.routeStop = routeStop;
        this.deliveryRequest = deliveryRequest;
    }

    /**
     * 정류장 설정 (양방향 관계용)
     */
    protected void setRouteStop(RouteStop routeStop) {
        this.routeStop = routeStop;
    }
}
