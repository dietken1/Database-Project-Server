package backend.databaseproject.domain.order.controller;

import backend.databaseproject.domain.order.dto.request.OrderCreateRequest;
import backend.databaseproject.domain.order.dto.response.OrderResponse;
import backend.databaseproject.domain.order.service.OrderService;
import backend.databaseproject.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 API Controller
 * 주문 생성, 조회 등의 REST API를 제공합니다.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "주문 관련 API")
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성
     * 장바구니 내용을 기반으로 배송 요청을 생성합니다.
     */
    @PostMapping
    @Operation(
            summary = "주문 생성",
            description = "장바구니 내용을 기반으로 배송 요청을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "주문 생성 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (매장/고객 없음, 주문 항목 없음, 재고 부족 등)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    public ResponseEntity<BaseResponse<OrderResponse>> createOrder(
            @RequestBody @Valid OrderCreateRequest request
    ) {
        OrderResponse orderResponse = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(orderResponse));
    }

    /**
     * 주문 조회
     * 주문 상세 정보를 조회합니다.
     */
    @GetMapping("/{orderId}")
    @Operation(
            summary = "주문 조회",
            description = "주문 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "주문 조회 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "주문을 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    public ResponseEntity<BaseResponse<OrderResponse>> getOrder(
            @PathVariable("orderId")
            @Schema(description = "주문 ID", example = "1")
            Long requestId
    ) {
        OrderResponse orderResponse = orderService.getOrder(requestId);
        return ResponseEntity.ok(BaseResponse.success(orderResponse));
    }
}
