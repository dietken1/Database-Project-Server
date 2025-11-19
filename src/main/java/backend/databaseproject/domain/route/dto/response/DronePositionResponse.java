package backend.databaseproject.domain.route.dto.response;

import backend.databaseproject.domain.route.entity.RoutePosition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 드론 위치 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "드론 현재 위치 정보")
public class DronePositionResponse {

    @Schema(description = "경로 ID", example = "1")
    private Long routeId;

    @Schema(description = "위도", example = "37.123456")
    private BigDecimal lat;

    @Schema(description = "경도", example = "127.123456")
    private BigDecimal lng;

    @Schema(description = "고도 (미터)", example = "50.00")
    private BigDecimal altitudeM;

    @Schema(description = "속도 (km/h)", example = "30.00")
    private BigDecimal speedKmh;

    @Schema(description = "배터리 잔량 (%)", example = "85")
    private Integer battery;

    @Schema(description = "기록 시각", example = "2024-01-15T14:30:00")
    private LocalDateTime recordedAt;

    /**
     * Entity를 DTO로 변환
     */
    public static DronePositionResponse from(RoutePosition routePosition) {
        if (routePosition == null) {
            return null;
        }

        return DronePositionResponse.builder()
                .routeId(routePosition.getRoute().getRouteId())
                .lat(routePosition.getLat())
                .lng(routePosition.getLng())
                .altitudeM(routePosition.getAltitudeM())
                .speedKmh(routePosition.getSpeedKmh())
                .battery(routePosition.getBattery())
                .recordedAt(routePosition.getRecordedAt())
                .build();
    }
}
