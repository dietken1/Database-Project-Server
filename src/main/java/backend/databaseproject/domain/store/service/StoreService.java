package backend.databaseproject.domain.store.service;

import backend.databaseproject.domain.store.dto.response.CategoryResponse;
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

    /**
     * 사용자 위치 기반 주변 매장 조회
     *
     * @param lat      사용자 위도
     * @param lng      사용자 경도
     * @param radiusKm 검색 반경 (km)
     * @return 주변 매장 목록
     */
    public List<StoreResponse> getStoresNearby(BigDecimal lat, BigDecimal lng, BigDecimal radiusKm) {
        // 데이터베이스에서 반경 내의 활성 매장 조회
        List<Store> stores = storeRepository.findStoresWithinRadius(lat, lng, radiusKm);

        // 각 매장의 거리를 계산하여 DTO로 변환
        return stores.stream()
                .map(store -> {
                    double distance = GeoUtils.calculateDistance(
                            lat.doubleValue(),
                            lng.doubleValue(),
                            store.getLat().doubleValue(),
                            store.getLng().doubleValue()
                    );
                    return StoreResponse.from(store, distance);
                })
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
}
