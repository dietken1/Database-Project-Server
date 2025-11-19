package backend.databaseproject.domain.order.service;

import backend.databaseproject.domain.customer.entity.Customer;
import backend.databaseproject.domain.customer.repository.CustomerRepository;
import backend.databaseproject.domain.order.dto.request.OrderCreateRequest;
import backend.databaseproject.domain.order.dto.request.OrderItemRequest;
import backend.databaseproject.domain.order.dto.response.OrderResponse;
import backend.databaseproject.domain.order.entity.DeliveryRequest;
import backend.databaseproject.domain.order.entity.RequestItem;
import backend.databaseproject.domain.order.repository.DeliveryRequestRepository;
import backend.databaseproject.domain.order.repository.RequestItemRepository;
import backend.databaseproject.domain.product.entity.Product;
import backend.databaseproject.domain.product.repository.ProductRepository;
import backend.databaseproject.domain.store.entity.Store;
import backend.databaseproject.domain.store.entity.StoreProduct;
import backend.databaseproject.domain.store.repository.StoreProductRepository;
import backend.databaseproject.domain.store.repository.StoreRepository;
import backend.databaseproject.global.common.BaseException;
import backend.databaseproject.global.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문 서비스
 * 주문 생성, 조회 등의 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final DeliveryRequestRepository deliveryRequestRepository;
    private final RequestItemRepository requestItemRepository;
    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StoreProductRepository storeProductRepository;

    /**
     * 주문 생성
     * 1. Store 조회 (없으면 STORE_NOT_FOUND)
     * 2. Store 활성 확인 (비활성이면 STORE_NOT_ACTIVE)
     * 3. Customer 조회 (없으면 CUSTOMER_NOT_FOUND)
     * 4. items가 비어있으면 ORDER_ITEMS_EMPTY
     * 5. 각 아이템별로:
     *    - StoreProduct 조회 (매장에서 판매하는 상품인지 확인)
     *    - 재고 확인 (부족하면 PRODUCT_OUT_OF_STOCK)
     *    - 최대 수량 확인 (초과하면 PRODUCT_EXCEED_MAX_QUANTITY)
     * 6. totalWeightKg, totalAmount, itemCount 계산
     * 7. DeliveryRequest 생성 (originLat/Lng는 Store, destLat/Lng는 Customer)
     * 8. RequestItem들 생성하여 추가
     * 9. 재고 감소 (storeProduct.decreaseStock(quantity))
     * 10. 저장 후 OrderResponse 반환
     */
    public OrderResponse createOrder(OrderCreateRequest request) {
        // 1. Store 조회
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));

        // 2. Store 활성 확인
        if (!store.getIsActive()) {
            throw new BaseException(ErrorCode.STORE_NOT_ACTIVE);
        }

        // 3. Customer 조회
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BaseException(ErrorCode.CUSTOMER_NOT_FOUND));

        // 4. items 비어있는지 확인
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BaseException(ErrorCode.ORDER_ITEMS_EMPTY);
        }

        // 5. 각 아이템별로 검증 및 정보 수집
        BigDecimal totalWeightKg = BigDecimal.ZERO;
        Integer totalAmount = 0;
        int itemCount = request.getItems().size();

        for (OrderItemRequest itemRequest : request.getItems()) {
            // StoreProduct 조회 (매장에서 판매하는 상품인지 확인)
            StoreProduct storeProduct = storeProductRepository.findById(
                    new StoreProduct.StoreProductId(request.getStoreId(), itemRequest.getProductId())
            ).orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_NOT_FOUND));

            // 활성화된 상품인지 확인
            if (!storeProduct.getIsActive()) {
                throw new BaseException(ErrorCode.PRODUCT_NOT_ACTIVE);
            }

            // 재고 확인
            if (storeProduct.getStockQty() < itemRequest.getQuantity()) {
                throw new BaseException(ErrorCode.PRODUCT_OUT_OF_STOCK);
            }

            // 최대 수량 확인
            if (itemRequest.getQuantity() > storeProduct.getMaxQtyPerOrder()) {
                throw new BaseException(ErrorCode.PRODUCT_EXCEED_MAX_QUANTITY);
            }

            // 총 금액 계산
            totalAmount += storeProduct.getPrice() * itemRequest.getQuantity();

            // 총 무게 계산
            BigDecimal itemWeightKg = storeProduct.getProduct().getUnitWeightKg()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalWeightKg = totalWeightKg.add(itemWeightKg);
        }

        // 6. DeliveryRequest 생성
        // originLat/Lng는 Store, destLat/Lng는 Customer
        DeliveryRequest deliveryRequest = DeliveryRequest.builder()
                .store(store)
                .customer(customer)
                .originLat(store.getLat())
                .originLng(store.getLng())
                .destLat(customer.getLat())
                .destLng(customer.getLng())
                .totalWeightKg(totalWeightKg)
                .totalAmount(totalAmount)
                .itemCount(itemCount)
                .note(request.getNote())
                .build();

        // 7. DeliveryRequest 저장
        DeliveryRequest savedDeliveryRequest = deliveryRequestRepository.save(deliveryRequest);

        // 8. RequestItem들 생성 및 추가
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_NOT_FOUND));

            StoreProduct storeProduct = storeProductRepository.findById(
                    new StoreProduct.StoreProductId(request.getStoreId(), itemRequest.getProductId())
            ).orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_NOT_FOUND));

            RequestItem requestItem = RequestItem.builder()
                    .deliveryRequest(savedDeliveryRequest)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(storeProduct.getPrice())
                    .unitWeightKg(product.getUnitWeightKg())
                    .build();

            requestItemRepository.save(requestItem);
            savedDeliveryRequest.addRequestItem(requestItem);

            // 9. 재고 감소
            storeProduct.decreaseStock(itemRequest.getQuantity());
        }

        // 10. 저장 후 OrderResponse 반환
        return OrderResponse.from(savedDeliveryRequest);
    }

    /**
     * 주문 조회
     * DeliveryRequest 조회 (없으면 ORDER_NOT_FOUND)
     * RequestItem들도 함께 fetch
     * OrderResponse로 변환하여 반환
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long requestId) {
        DeliveryRequest deliveryRequest = deliveryRequestRepository.findById(requestId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_NOT_FOUND));

        return OrderResponse.from(deliveryRequest);
    }
}
