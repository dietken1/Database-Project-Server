package backend.databaseproject.domain.route.entity;

import backend.databaseproject.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 배송 경로 정류장 - 주문 매핑 엔티티
 */
@Entity
@Table(name = "route_stop_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteStopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_stop_order_id")
    private Long routeStopOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private RouteStop routeStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public RouteStopOrder(RouteStop routeStop, Order order) {
        this.routeStop = routeStop;
        this.order = order;
    }

    /**
     * 정류장 설정 (양방향 관계용)
     */
    protected void setRouteStop(RouteStop routeStop) {
        this.routeStop = routeStop;
    }
}
