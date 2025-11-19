package backend.databaseproject.domain.order.dto.response;

import backend.databaseproject.domain.order.entity.DeliveryRequest;
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
    private Long requestId;

    @Schema(description = "매장 ID", example = "1")
    private Long storeId;

    @Schema(description = "매장명", example = "스타벅스 강남점")
    private String storeName;

    @Schema(description = "고객 ID", example = "1")
    private Long customerId;

    @Schema(description = "고객명", example = "김철수")
    private String customerName;

    @Schema(description = "배송 주소", example = "서울시 강남구 테헤란로 123")
    private String customerAddress;

    @Schema(description = "총 무게(kg)", example = "1.500")
    private BigDecimal totalWeightKg;

    @Schema(description = "총 금액(원)", example = "15000")
    private Integer totalAmount;

    @Schema(description = "주문 항목 수", example = "3")
    private Integer itemCount;

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

    /**
     * DeliveryRequest 엔티티로부터 Response 생성 (Factory Method)
     */
    public static OrderResponse from(DeliveryRequest deliveryRequest) {
        List<OrderItemResponse> items = deliveryRequest.getRequestItems()
                .stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.builder()
                .requestId(deliveryRequest.getRequestId())
                .storeId(deliveryRequest.getStore().getStoreId())
                .storeName(deliveryRequest.getStore().getName())
                .customerId(deliveryRequest.getCustomer().getCustomerId())
                .customerName(deliveryRequest.getCustomer().getName())
                .customerAddress(deliveryRequest.getCustomer().getAddress())
                .totalWeightKg(deliveryRequest.getTotalWeightKg())
                .totalAmount(deliveryRequest.getTotalAmount())
                .itemCount(deliveryRequest.getItemCount())
                .status(deliveryRequest.getStatus().toString())
                .createdAt(deliveryRequest.getCreatedAt())
                .assignedAt(deliveryRequest.getAssignedAt())
                .completedAt(deliveryRequest.getCompletedAt())
                .items(items)
                .note(deliveryRequest.getNote())
                .build();
    }
}
