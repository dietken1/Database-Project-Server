package backend.databaseproject.domain.store.controller;

import backend.databaseproject.domain.store.dto.response.CategoryResponse;
import backend.databaseproject.domain.store.dto.response.ProductResponse;
import backend.databaseproject.domain.store.dto.response.StoreResponse;
import backend.databaseproject.domain.store.service.StoreService;
import backend.databaseproject.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 매장 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
@Tag(name = "Store", description = "매장 관련 API")
public class StoreController {

    private final StoreService storeService;

    /**
     * 주변 매장 조회
     * 사용자 위치를 기반으로 지정된 반경 내의 활성화된 매장 목록을 조회합니다.
     *
     * @param lat    사용자 위도 (필수)
     * @param lng    사용자 경도 (필수)
     * @param radius 검색 반경 (km, 선택, 기본값: 5.0)
     * @return 주변 매장 목록
     */
    @GetMapping
    @Operation(
            summary = "주변 매장 조회",
            description = "사용자 위치 기반으로 주변 매장 목록을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = BaseResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (필수 파라미터 누락 등)"
                    )
            }
    )
    public BaseResponse<List<StoreResponse>> getNearbyStores(
            @Parameter(name = "lat", description = "위도", required = true, example = "37.4979")
            @RequestParam BigDecimal lat,

            @Parameter(name = "lng", description = "경도", required = true, example = "127.0276")
            @RequestParam BigDecimal lng,

            @Parameter(name = "radius", description = "검색 반경 (km)", example = "5.0")
            @RequestParam(required = false, defaultValue = "5.0") BigDecimal radius
    ) {
        List<StoreResponse> stores = storeService.getStoresNearby(lat, lng, radius);
        return BaseResponse.success(stores);
    }

    /**
     * 매장 카테고리 목록 조회
     * 특정 매장에서 판매 중인 상품의 카테고리 목록을 조회합니다.
     *
     * @param storeId 매장 ID
     * @return 카테고리 목록
     */
    @GetMapping("/{storeId}/categories")
    @Operation(
            summary = "매장 카테고리 목록 조회",
            description = "특정 매장에서 판매 중인 상품의 카테고리 목록을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = BaseResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "존재하지 않는 매장"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "운영 중이지 않은 매장"
                    )
            }
    )
    public BaseResponse<List<CategoryResponse>> getCategories(
            @Parameter(name = "storeId", description = "매장 ID", required = true, example = "1")
            @PathVariable Long storeId
    ) {
        List<CategoryResponse> categories = storeService.getCategories(storeId);
        return BaseResponse.success(categories);
    }

    /**
     * 매장 상품 목록 조회
     * 특정 매장의 상품 목록을 조회합니다.
     * 카테고리가 지정된 경우 해당 카테고리의 상품만 조회하고,
     * 지정되지 않은 경우 모든 판매 중인 상품을 조회합니다.
     *
     * @param storeId  매장 ID
     * @param category 카테고리 (선택)
     * @return 상품 목록
     */
    @GetMapping("/{storeId}/products")
    @Operation(
            summary = "매장 상품 목록 조회",
            description = "카테고리별 또는 전체 상품 목록을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = BaseResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "존재하지 않는 매장"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "운영 중이지 않은 매장"
                    )
            }
    )
    public BaseResponse<List<ProductResponse>> getProducts(
            @Parameter(name = "storeId", description = "매장 ID", required = true, example = "1")
            @PathVariable Long storeId,

            @Parameter(name = "category", description = "카테고리 (선택)", example = "음료")
            @RequestParam(required = false) String category
    ) {
        List<ProductResponse> products = storeService.getProducts(storeId, category);
        return BaseResponse.success(products);
    }
}
