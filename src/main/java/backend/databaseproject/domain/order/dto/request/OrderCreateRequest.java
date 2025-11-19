package backend.databaseproject.domain.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 주문 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "주문 생성 요청 정보")
public class OrderCreateRequest {

    @NotNull(message = "매장 ID는 필수입니다.")
    @Schema(description = "매장 ID", example = "1")
    private Long storeId;

    @NotNull(message = "고객 ID는 필수입니다.")
    @Schema(description = "고객 ID", example = "1")
    private Long customerId;

    @NotEmpty(message = "주문 항목이 비어있을 수 없습니다.")
    @Valid
    @Schema(description = "주문 항목 목록")
    private List<OrderItemRequest> items;

    @Schema(description = "주문 메모", example = "조심스럽게 배송해주세요.", nullable = true)
    private String note;
}
