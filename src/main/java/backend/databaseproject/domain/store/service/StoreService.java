package backend.databaseproject.domain.store.service;

import backend.databaseproject.domain.drone.repository.DroneRepository;
import backend.databaseproject.domain.store.dto.response.CategoryResponse;
import backend.databaseproject.domain.store.dto.response.DeliveryInfoResponse;
import backend.databaseproject.domain.store.dto.response.ProductResponse;
import backend.databaseproject.domain.store.dto.response.StoreResponse;
import backend.databaseproject.domain.store.entity.Store;
import backend.databaseproject.domain.store.entity.StoreProduct;
import backend.databaseproject.domain.store.repository.StoreProductRepository;
import backend.databaseproject.domain.store.repository.StoreRepository;
import backend.databaseproject.global.common.BaseException;
import backend.databaseproject.global.common.ErrorCode;
import backend.databaseproject.global.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 매장 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreProductRepository storeProductRepository;
    private final DroneRepository droneRepository;

    /**
     * 사용자 위치 기반 배달 가능한 매장 조회
     * 각 매장의 배달 가능 거리(deliveryRadiusKm) 내에 사용자가 있는 매장만 반환합니다.
     *
     * @param lat 사용자 위도
     * @param lng 사용자 경도
     * @return 배달 가능한 매장 목록 (거리순 정렬)
     */
    public List<StoreResponse> getStoresNearby(BigDecimal lat, BigDecimal lng) {
        // 성능 최적화를 위해 합리적인 최대 반경(50km) 내의 활성 매장만 조회
        BigDecimal maxRadiusKm = BigDecimal.valueOf(50.0);
        List<Store> stores = storeRepository.findStoresWithinRadius(lat, lng, maxRadiusKm);

        // 각 매장의 거리를 계산하고, 매장의 배달 가능 거리 내에 있는 매장만 필터링
        return stores.stream()
                .map(store -> {
                    double distanceKm = GeoUtils.calculateDistance(
                            lat.doubleValue(),
                            lng.doubleValue(),
                            store.getLat().doubleValue(),
                            store.getLng().doubleValue()
                    );
                    return new Object() {
                        final Store storeData = store;
                        final double distanceData = distanceKm;
                    };
                })
                // 사용자가 매장의 배달 가능 거리 내에 있는 경우만 포함
                .filter(obj -> obj.distanceData <= obj.storeData.getDeliveryRadiusKm().doubleValue())
                .sorted((a, b) -> Double.compare(a.distanceData, b.distanceData))
                .map(obj -> StoreResponse.from(obj.storeData, obj.distanceData))
                .collect(Collectors.toList());
    }

    /**
     * 특정 매장의 카테고리 목록 조회
     *
     * @param storeId 매장 ID
     * @return 카테고리 목록
     * @throws BaseException STORE_NOT_FOUND, STORE_NOT_ACTIVE
     */
    public List<CategoryResponse> getCategories(Long storeId) {
        // 매장 존재 여부 확인
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));

        // 매장 활성화 여부 확인
        if (!store.getIsActive()) {
            throw new BaseException(ErrorCode.STORE_NOT_ACTIVE);
        }

        // 카테고리 목록 조회
        List<String> categories = storeProductRepository.findCategoriesByStoreId(storeId);

        return categories.stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 매장의 상품 목록 조회
     *
     * @param storeId  매장 ID
     * @param category 카테고리 (optional)
     * @return 상품 목록
     * @throws BaseException STORE_NOT_FOUND, STORE_NOT_ACTIVE
     */
    public List<ProductResponse> getProducts(Long storeId, String category) {
        // 매장 존재 여부 확인
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));

        // 매장 활성화 여부 확인
        if (!store.getIsActive()) {
            throw new BaseException(ErrorCode.STORE_NOT_ACTIVE);
        }

        // 카테고리 여부에 따라 상품 조회
        List<StoreProduct> products;
        if (category == null || category.isBlank()) {
            products = storeProductRepository.findActiveProductsByStoreId(storeId);
        } else {
            products = storeProductRepository.findActiveProductsByStoreIdAndCategory(storeId, category);
        }

        return products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 매장명으로 매장 검색
     * 사용자 위치가 제공된 경우, 각 매장의 배달 가능 거리 내에 있는 매장만 반환합니다.
     *
     * @param name 매장명 (부분 일치)
     * @param lat  사용자 위도 (거리 계산용, optional)
     * @param lng  사용자 경도 (거리 계산용, optional)
     * @return 검색된 매장 목록 (사용자 위치 제공 시 배달 가능한 매장만 포함)
     */
    public List<StoreResponse> searchStoresByName(String name, BigDecimal lat, BigDecimal lng) {
        List<Store> stores = storeRepository.findByNameContainingAndIsActiveTrue(name);

        if (lat != null && lng != null) {
            // 사용자 위치가 제공된 경우: 거리 계산 + 배달 가능 거리 필터링
            return stores.stream()
                    .map(store -> {
                        double distanceKm = GeoUtils.calculateDistance(
                                lat.doubleValue(),
                                lng.doubleValue(),
                                store.getLat().doubleValue(),
                                store.getLng().doubleValue()
                        );
                        return new Object() {
                            final Store storeData = store;
                            final double distanceData = distanceKm;
                        };
                    })
                    // 사용자가 매장의 배달 가능 거리 내에 있는 경우만 포함
                    .filter(obj -> obj.distanceData <= obj.storeData.getDeliveryRadiusKm().doubleValue())
                    .sorted((a, b) -> Double.compare(a.distanceData, b.distanceData))
                    .map(obj -> StoreResponse.from(obj.storeData, obj.distanceData))
                    .collect(Collectors.toList());
        } else {
            // 사용자 위치가 없는 경우: 모든 검색 결과 반환 (거리 정보 없음)
            return stores.stream()
                    .map(StoreResponse::from)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 배송 정보 조회
     * 프론트엔드에서 주문 전 검증에 필요한 정보 제공
     *
     * @param storeId 매장 ID
     * @param userLat 사용자 위도 (optional)
     * @param userLng 사용자 경도 (optional)
     * @return 배송 정보
     * @throws BaseException STORE_NOT_FOUND, STORE_NOT_ACTIVE
     */
    public DeliveryInfoResponse getDeliveryInfo(Long storeId, BigDecimal userLat, BigDecimal userLng) {
        // 매장 존재 여부 확인
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));

        // 매장 활성화 여부 확인
        if (!store.getIsActive()) {
            throw new BaseException(ErrorCode.STORE_NOT_ACTIVE);
        }

        // 시스템 드론 최대 무게 조회
        BigDecimal maxWeightKg = droneRepository.findMinMaxPayloadKg()
                .orElse(BigDecimal.valueOf(5.0)); // 기본값 5kg

        // 사용자 위치가 제공된 경우 거리 및 배송 가능 여부 계산
        BigDecimal distanceKm = null;
        Boolean isDeliverable = null;

        if (userLat != null && userLng != null) {
            double distance = GeoUtils.calculateDistance(
                    store.getLat().doubleValue(),
                    store.getLng().doubleValue(),
                    userLat.doubleValue(),
                    userLng.doubleValue()
            );
            distanceKm = BigDecimal.valueOf(distance);
            isDeliverable = distance <= store.getDeliveryRadiusKm().doubleValue();
        }

        return DeliveryInfoResponse.of(
                store.getStoreId(),
                store.getName(),
                store.getDeliveryRadiusKm(),
                maxWeightKg,
                isDeliverable,
                distanceKm
        );
    }

}
