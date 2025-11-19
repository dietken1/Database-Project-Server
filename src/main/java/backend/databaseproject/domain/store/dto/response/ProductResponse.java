package backend.databaseproject.domain.store.dto.response;

import backend.databaseproject.domain.store.entity.StoreProduct;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 상품 정보 응답 DTO
 */
@Schema(description = "상품 정보 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "초코우유")
    private String name;

    @Schema(description = "카테고리", example = "음료")
    private String category;

    @Schema(description = "단위 무게 (kg)", example = "0.200")
    private BigDecimal unitWeightKg;

    @Schema(description = "가격", example = "2500")
    private Integer price;

    @Schema(description = "재고 수량", example = "50")
    private Integer stockQty;

    @Schema(description = "최대 주문 수량", example = "10")
    private Integer maxQtyPerOrder;

    /**
     * StoreProduct 엔티티를 ProductResponse로 변환
     *
     * @param storeProduct 매장 상품 엔티티
     * @return ProductResponse
     */
    public static ProductResponse from(StoreProduct storeProduct) {
        return ProductResponse.builder()
                .productId(storeProduct.getProduct().getProductId())
                .name(storeProduct.getProduct().getName())
                .category(storeProduct.getProduct().getCategory())
                .unitWeightKg(storeProduct.getProduct().getUnitWeightKg())
                .price(storeProduct.getPrice())
                .stockQty(storeProduct.getStockQty())
                .maxQtyPerOrder(storeProduct.getMaxQtyPerOrder())
                .build();
    }

    /**
     * StoreProduct 엔티티를 ProductResponse로 변환
     *
     * @param storeProduct 매장 상품 엔티티
     * @return ProductResponse
     */
    public static ProductResponse of(StoreProduct storeProduct) {
        return from(storeProduct);
    }
}
