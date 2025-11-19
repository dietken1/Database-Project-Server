package backend.databaseproject.domain.drone.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 드론 엔티티
 */
@Entity
@Table(name = "drone")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Drone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "drone_id")
    private Long droneId;

    @Column(nullable = false, length = 40)
    private String model;

    @Column(name = "battery_capacity", nullable = false)
    private Integer batteryCapacity; // mAh

    @Column(name = "max_payload_kg", nullable = false, precision = 6, scale = 3)
    private BigDecimal maxPayloadKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DroneStatus status = DroneStatus.IDLE;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
    }

    @Builder
    public Drone(String model, Integer batteryCapacity, BigDecimal maxPayloadKg, DroneStatus status) {
        this.model = model;
        this.batteryCapacity = batteryCapacity;
        this.maxPayloadKg = maxPayloadKg;
        this.status = status != null ? status : DroneStatus.IDLE;
    }

    /**
     * 드론 상태 변경
     */
    public void changeStatus(DroneStatus newStatus) {
        this.status = newStatus;
    }
}
