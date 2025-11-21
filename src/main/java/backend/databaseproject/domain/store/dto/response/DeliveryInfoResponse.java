package backend.databaseproject.domain.store.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 배송 정보 응답 DTO
 * 프론트엔드에서 주문 전 검증에 필요한 정보를 제공합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "배송 정보")
public class DeliveryInfoResponse {

    @Schema(description = "매장 ID", example = "1")
    private Long storeId;

    @Schema(description = "매장명", example = "스타벅스 강남점")
    private String storeName;

    @Schema(description = "매장 배송 가능 반경 (km)", example = "2.00")
    private BigDecimal deliveryRadiusKm;

    @Schema(description = "시스템 드론 최대 적재 무게 (kg)", example = "5.000")
    private BigDecimal maxWeightKg;

    @Schema(description = "사용자 주소가 배송 가능 범위 내에 있는지 여부", example = "true")
    private Boolean isDeliverable;

    @Schema(description = "매장-사용자 간 거리 (km)", example = "1.50", nullable = true)
    private BigDecimal distanceKm;

    /**
     * Factory Method
     */
    public static DeliveryInfoResponse of(Long storeId, String storeName,
                                          BigDecimal deliveryRadiusKm, BigDecimal maxWeightKg,
                                          Boolean isDeliverable, BigDecimal distanceKm) {
        return DeliveryInfoResponse.builder()
                .storeId(storeId)
                .storeName(storeName)
                .deliveryRadiusKm(deliveryRadiusKm)
                .maxWeightKg(maxWeightKg)
                .isDeliverable(isDeliverable)
                .distanceKm(distanceKm)
                .build();
    }
}
