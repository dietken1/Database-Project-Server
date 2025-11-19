package backend.databaseproject.domain.order.dto.response;

import backend.databaseproject.domain.order.entity.RequestItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 주문 항목 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "주문 항목 상세 정보")
public class OrderItemResponse {

    @Schema(description = "주문 항목 ID", example = "1")
    private Long requestItemId;

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "아메리카노")
    private String productName;

    @Schema(description = "주문 수량", example = "2")
    private Integer quantity;

    @Schema(description = "단가", example = "3000")
    private Integer unitPrice;

    @Schema(description = "단위 무게(kg)", example = "0.300")
    private BigDecimal unitWeightKg;

    @Schema(description = "소계 (수량 * 단가)", example = "6000")
    private Integer subtotal;

    /**
     * RequestItem 엔티티로부터 Response 생성 (Factory Method)
     */
    public static OrderItemResponse from(RequestItem requestItem) {
        Integer subtotal = requestItem.getQuantity() * requestItem.getUnitPrice();

        return OrderItemResponse.builder()
                .requestItemId(requestItem.getRequestItemId())
                .productId(requestItem.getProduct().getProductId())
                .productName(requestItem.getProduct().getName())
                .quantity(requestItem.getQuantity())
                .unitPrice(requestItem.getUnitPrice())
                .unitWeightKg(requestItem.getUnitWeightKg())
                .subtotal(subtotal)
                .build();
    }
}
