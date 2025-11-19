package backend.databaseproject.domain.store.dto.response;

import backend.databaseproject.domain.store.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 매장 정보 응답 DTO
 */
@Schema(description = "매장 정보 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {

    @Schema(description = "매장 ID", example = "1")
    private Long storeId;

    @Schema(description = "매장명", example = "편의점A")
    private String name;

    @Schema(description = "매장 유형", example = "CONVENIENCE")
    private String type;

    @Schema(description = "전화번호", example = "02-1234-5678")
    private String phone;

    @Schema(description = "주소", example = "서울시 강남구 테헤란로 123")
    private String address;

    @Schema(description = "위도", example = "37.494095")
    private BigDecimal lat;

    @Schema(description = "경도", example = "127.027610")
    private BigDecimal lng;

    @Schema(description = "배송 가능 반경 (km)", example = "2.00")
    private BigDecimal deliveryRadiusKm;

    @Schema(description = "사용자로부터의 거리 (km)", example = "1.50")
    private Double distanceKm;

    /**
     * Store 엔티티를 StoreResponse로 변환
     *
     * @param store      매장 엔티티
     * @param distanceKm 사용자로부터의 거리
     * @return StoreResponse
     */
    public static StoreResponse from(Store store, Double distanceKm) {
        return StoreResponse.builder()
                .storeId(store.getStoreId())
                .name(store.getName())
                .type(store.getType().name())
                .phone(store.getPhone())
                .address(store.getAddress())
                .lat(store.getLat())
                .lng(store.getLng())
                .deliveryRadiusKm(store.getDeliveryRadiusKm())
                .distanceKm(distanceKm)
                .build();
    }

    /**
     * Store 엔티티를 StoreResponse로 변환 (거리 없음)
     *
     * @param store 매장 엔티티
     * @return StoreResponse
     */
    public static StoreResponse from(Store store) {
        return StoreResponse.builder()
                .storeId(store.getStoreId())
                .name(store.getName())
                .type(store.getType().name())
                .phone(store.getPhone())
                .address(store.getAddress())
                .lat(store.getLat())
                .lng(store.getLng())
                .deliveryRadiusKm(store.getDeliveryRadiusKm())
                .distanceKm(null)
                .build();
    }
}
