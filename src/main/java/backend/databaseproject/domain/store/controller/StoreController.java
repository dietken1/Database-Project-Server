package backend.databaseproject.domain.store.controller;

import backend.databaseproject.domain.store.dto.response.CategoryResponse;
import backend.databaseproject.domain.store.dto.response.DeliveryInfoResponse;
import backend.databaseproject.domain.store.dto.response.ProductResponse;
import backend.databaseproject.domain.store.dto.response.StoreResponse;
import backend.databaseproject.domain.store.service.StoreService;
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
     * 사용자 위치를 기반으로 각 매장의 배달 가능 거리 내에 있는 활성화된 매장 목록을 조회합니다.
     *
     * @param lat 사용자 위도 (필수)
     * @param lng 사용자 경도 (필수)
     * @return 주변 매장 목록 (배달 가능한 매장만 반환)
     */
    @GetMapping
    @Operation(
            summary = "주변 매장 조회",
            description = "사용자 위치 기반으로 배달 가능한 매장 목록을 조회합니다. 각 매장의 배달 가능 거리 내에 있는 매장만 반환됩니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (필수 파라미터 누락 등)"
                    )
            }
    )
    public List<StoreResponse> getNearbyStores(
            @Parameter(name = "lat", description = "위도", required = true, example = "37.4979")
            @RequestParam BigDecimal lat,

            @Parameter(name = "lng", description = "경도", required = true, example = "127.0276")
            @RequestParam BigDecimal lng
    ) {
        List<StoreResponse> stores = storeService.getStoresNearby(lat, lng);
        return stores;
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
                            description = "조회 성공"
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
    public List<CategoryResponse> getCategories(
            @Parameter(name = "storeId", description = "매장 ID", required = true, example = "1")
            @PathVariable Long storeId
    ) {
        List<CategoryResponse> categories = storeService.getCategories(storeId);
        return categories;
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
                            description = "조회 성공"
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
    public List<ProductResponse> getProducts(
            @Parameter(name = "storeId", description = "매장 ID", required = true, example = "1")
            @PathVariable Long storeId,

            @Parameter(name = "category", description = "카테고리 (선택)", example = "음료")
            @RequestParam(required = false) String category
    ) {
        List<ProductResponse> products = storeService.getProducts(storeId, category);
        return products;
    }

    /**
     * 매장 검색 (매장명)
     * 매장명으로 매장을 검색합니다 (부분 일치).
     * 사용자 위치가 제공되면 거리 정보도 함께 반환합니다.
     *
     * @param name 매장명 (부분 일치)
     * @param lat  사용자 위도 (선택)
     * @param lng  사용자 경도 (선택)
     * @return 검색된 매장 목록
     */
    @GetMapping("/search")
    @Operation(
            summary = "매장 검색",
            description = "매장명으로 매장을 검색합니다 (부분 일치). 사용자 위치 정보를 제공하면 거리도 함께 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공"
                    )
            }
    )
    public List<StoreResponse> searchStores(
            @Parameter(name = "name", description = "매장명 (부분 일치)", required = true, example = "편의점")
            @RequestParam String name,

            @Parameter(name = "lat", description = "위도 (선택)", example = "37.4979")
            @RequestParam(required = false) BigDecimal lat,

            @Parameter(name = "lng", description = "경도 (선택)", example = "127.0276")
            @RequestParam(required = false) BigDecimal lng
    ) {
        List<StoreResponse> stores = storeService.searchStoresByName(name, lat, lng);
        return stores;
    }

    /**
     * 배송 정보 조회
     * 프론트엔드에서 주문 전 검증에 필요한 정보를 제공합니다.
     * 드론 최대 적재 무게, 매장 배송 반경, 배송 가능 여부 등을 반환합니다.
     *
     * @param storeId 매장 ID
     * @param lat     사용자 위도 (선택)
     * @param lng     사용자 경도 (선택)
     * @return 배송 정보
     */
    @GetMapping("/{storeId}/delivery-info")
    @Operation(
            summary = "배송 정보 조회",
            description = "주문 전 검증에 필요한 배송 정보를 조회합니다. " +
                         "드론 최대 적재 무게, 매장 배송 반경, 사용자 위치 기반 배송 가능 여부를 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공"
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
    public DeliveryInfoResponse getDeliveryInfo(
            @Parameter(name = "storeId", description = "매장 ID", required = true, example = "1")
            @PathVariable Long storeId,

            @Parameter(name = "lat", description = "사용자 위도 (선택)", example = "37.4979")
            @RequestParam(required = false) BigDecimal lat,

            @Parameter(name = "lng", description = "사용자 경도 (선택)", example = "127.0276")
            @RequestParam(required = false) BigDecimal lng
    ) {
        DeliveryInfoResponse deliveryInfo = storeService.getDeliveryInfo(storeId, lat, lng);
        return deliveryInfo;
    }

}
