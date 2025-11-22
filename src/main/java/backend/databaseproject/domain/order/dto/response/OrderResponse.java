package backend.databaseproject.domain.order.dto.response;

import backend.databaseproject.domain.order.entity.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "주문 상세 정보")
public class OrderResponse {

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "매장 ID", example = "1")
    private Long storeId;

    @Schema(description = "매장명", example = "스타벅스 강남점")
    private String storeName;

    @Schema(description = "총 금액(원)", example = "15000")
    private Integer totalAmount;

    @Schema(description = "주문 상태", example = "CREATED")
    private String status;

    @Schema(description = "주문 생성 시간")
    private LocalDateTime createdAt;

    @Schema(description = "배송 할당 시간", nullable = true)
    private LocalDateTime assignedAt;

    @Schema(description = "배송 완료 시간", nullable = true)
    private LocalDateTime completedAt;

    @Schema(description = "주문 항목 목록")
    private List<OrderItemResponse> items;

    @Schema(description = "주문 메모", example = "조심스럽게 배송해주세요.", nullable = true)
    private String note;

    @Schema(description = "배송 경로 ID (배송 할당 후 드론 추적에 사용)", example = "1", nullable = true)
    private Long routeId;

    /**
     * Order 엔티티로부터 Response 생성 (Factory Method)
     */
    public static OrderResponse from(Order order, Long routeId) {
        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .storeId(order.getStore().getStoreId())
                .storeName(order.getStore().getName())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().toString())
                .createdAt(order.getCreatedAt())
                .assignedAt(order.getAssignedAt())
                .completedAt(order.getCompletedAt())
                .items(items)
                .note(order.getNote())
                .routeId(routeId)
                .build();
    }
}
