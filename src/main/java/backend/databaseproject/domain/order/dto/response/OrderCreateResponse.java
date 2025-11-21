package backend.databaseproject.domain.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 생성 응답 DTO
 * 주문 생성 시 최소한의 정보만 반환합니다.
 * 상세 정보는 주문 조회 API를 통해 확인할 수 있습니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "주문 생성 응답")
public class OrderCreateResponse {

    @Schema(description = "주문 ID", example = "1")
    private Long requestId;

    /**
     * 주문 ID로부터 Response 생성 (Factory Method)
     */
    public static OrderCreateResponse of(Long requestId) {
        return OrderCreateResponse.builder()
                .requestId(requestId)
                .build();
    }
}
