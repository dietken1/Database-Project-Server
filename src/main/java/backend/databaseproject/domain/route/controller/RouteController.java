package backend.databaseproject.domain.route.controller;

import backend.databaseproject.domain.route.dto.response.DronePositionResponse;
import backend.databaseproject.domain.route.dto.response.RouteResponse;
import backend.databaseproject.domain.route.service.RouteService;
import backend.databaseproject.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 배송 경로 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Tag(name = "Route", description = "배송 경로 관련 API")
@Slf4j
public class RouteController {

    private final RouteService routeService;

    /**
     * 경로 상세 조회
     * 특정 배송 경로의 상세 정보를 조회합니다.
     *
     * @param routeId 경로 ID
     * @return 경로 상세 정보
     */
    @GetMapping("/{routeId}")
    @Operation(
            summary = "경로 조회",
            description = "특정 배송 경로의 상세 정보를 조회합니다. 경로에 포함된 모든 정류장 정보를 함께 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = BaseResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "존재하지 않는 배송 경로"
                    )
            }
    )
    public BaseResponse<RouteResponse> getRoute(
            @Parameter(name = "routeId", description = "경로 ID", required = true, example = "1")
            @PathVariable Long routeId
    ) {
        log.info("API 호출: GET /api/routes/{}", routeId);
        RouteResponse route = routeService.getRoute(routeId);
        return BaseResponse.success(route);
    }

    /**
     * 드론 현재 위치 조회
     * 특정 경로의 드론 현재 위치를 조회합니다.
     *
     * @param routeId 경로 ID
     * @return 드론 현재 위치 정보
     */
    @GetMapping("/{routeId}/current-position")
    @Operation(
            summary = "드론 현재 위치 조회",
            description = "특정 배송 경로의 드론 현재 위치를 실시간으로 조회합니다. " +
                         "위도, 경도, 고도, 속도, 배터리 잔량 등의 정보를 포함합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = BaseResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "존재하지 않는 배송 경로 또는 위치 정보 없음"
                    )
            }
    )
    public BaseResponse<DronePositionResponse> getCurrentPosition(
            @Parameter(name = "routeId", description = "경로 ID", required = true, example = "1")
            @PathVariable Long routeId
    ) {
        log.info("API 호출: GET /api/routes/{}/current-position", routeId);
        DronePositionResponse position = routeService.getCurrentPosition(routeId);
        return BaseResponse.success(position);
    }

    /**
     * 진행 중인 배송 목록 조회
     * 현재 진행 중인 모든 배송 경로 목록을 조회합니다.
     *
     * @return 진행 중인 배송 경로 목록
     */
    @GetMapping("/active")
    @Operation(
            summary = "진행 중인 배송 목록 조회",
            description = "현재 진행 중인 모든 배송 경로 목록을 조회합니다. " +
                         "LAUNCHED 또는 IN_PROGRESS 상태인 경로들을 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = BaseResponse.class))
                    )
            }
    )
    public BaseResponse<List<RouteResponse>> getActiveRoutes() {
        log.info("API 호출: GET /api/routes/active");
        List<RouteResponse> activeRoutes = routeService.getActiveRoutes();
        return BaseResponse.success(activeRoutes);
    }
}
