package backend.databaseproject.domain.order.service;

import backend.databaseproject.domain.drone.repository.DroneRepository;
import backend.databaseproject.domain.order.dto.request.OrderCreateRequest;
import backend.databaseproject.domain.order.dto.request.OrderItemRequest;
import backend.databaseproject.domain.order.dto.response.OrderCreateResponse;
import backend.databaseproject.domain.order.dto.response.OrderResponse;
import backend.databaseproject.domain.order.entity.Order;
import backend.databaseproject.domain.order.entity.OrderItem;
import backend.databaseproject.domain.order.entity.OrderStatus;
import backend.databaseproject.domain.order.repository.OrderRepository;
import backend.databaseproject.domain.order.repository.OrderItemRepository;
import backend.databaseproject.domain.product.entity.Product;
import backend.databaseproject.domain.product.repository.ProductRepository;
import backend.databaseproject.domain.route.repository.RouteStopOrderRepository;
import backend.databaseproject.domain.store.entity.Store;
import backend.databaseproject.domain.store.entity.StoreProduct;
import backend.databaseproject.domain.store.repository.StoreProductRepository;
import backend.databaseproject.domain.store.repository.StoreRepository;
import backend.databaseproject.domain.user.entity.User;
import backend.databaseproject.domain.user.repository.UserRepository;
import backend.databaseproject.global.common.BaseException;
import backend.databaseproject.global.common.ErrorCode;
import backend.databaseproject.global.util.GeoUtils;
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

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StoreProductRepository storeProductRepository;
    private final DroneRepository droneRepository;
    private final RouteStopOrderRepository routeStopOrderRepository;

    /**
     * 주문 생성
     * 1. Store 조회 (없으면 STORE_NOT_FOUND)
     * 2. Store 활성 확인 (비활성이면 STORE_NOT_ACTIVE)
     * 3. User 조회 (없으면 USER_NOT_FOUND)
     * 4. items가 비어있으면 ORDER_ITEMS_EMPTY
     * 5. 각 아이템별로:
     *    - StoreProduct 조회 (매장에서 판매하는 상품인지 확인)
     *    - 재고 확인 (부족하면 PRODUCT_OUT_OF_STOCK)
     *    - 최대 수량 확인 (초과하면 PRODUCT_EXCEED_MAX_QUANTITY)
     *    - totalWeightKg, totalAmount, itemCount 계산
     * 6. 드론 최대 적재 무게 검증 (초과하면 ORDER_TOTAL_WEIGHT_EXCEEDED)
     * 7. 배송 가능 거리 검증 (초과하면 STORE_OUT_OF_DELIVERY_RANGE)
     * 8. Order 생성 (originLat/Lng는 Store, destLat/Lng는 Customer)
     * 9. Order 저장
     * 10. OrderItem들 생성하여 추가
     * 11. 재고 감소 (storeProduct.decreaseStock(quantity))
     * 12. 저장 후 OrderCreateResponse 반환 (orderId만 포함)
     */
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        // 1. Store 조회
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));

        // 2. Store 활성 확인
        if (!store.getIsActive()) {
            throw new BaseException(ErrorCode.STORE_NOT_ACTIVE);
        }

        // 3. User 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

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

        // 6. 드론 최대 적재 무게 검증
        BigDecimal maxPayloadKg = droneRepository.findMinMaxPayloadKg()
                .orElse(BigDecimal.valueOf(5.0)); // 기본값 5kg
        if (totalWeightKg.compareTo(maxPayloadKg) > 0) {
            throw new BaseException(ErrorCode.ORDER_TOTAL_WEIGHT_EXCEEDED);
        }

        // 7. 배송 가능 거리 검증
        double distanceKm = GeoUtils.calculateDistance(
                store.getLat().doubleValue(),
                store.getLng().doubleValue(),
                user.getLat().doubleValue(),
                user.getLng().doubleValue()
        );
        if (distanceKm > store.getDeliveryRadiusKm().doubleValue()) {
            throw new BaseException(ErrorCode.STORE_OUT_OF_DELIVERY_RANGE);
        }

        // 8. Order 생성
        // originLat/Lng는 Store, destLat/Lng는 User
        Order order = Order.builder()
                .store(store)
                .user(user)
                .originLat(store.getLat())
                .originLng(store.getLng())
                .destLat(user.getLat())
                .destLng(user.getLng())
                .totalWeightKg(totalWeightKg)
                .totalAmount(totalAmount)
                .itemCount(itemCount)
                .note(request.getNote())
                .build();

        // 9. Order 저장
        Order savedOrder = orderRepository.save(order);

        // 10. OrderItem들 생성 및 추가
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_NOT_FOUND));

            StoreProduct storeProduct = storeProductRepository.findById(
                    new StoreProduct.StoreProductId(request.getStoreId(), itemRequest.getProductId())
            ).orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_NOT_FOUND));

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(storeProduct.getPrice())
                    .unitWeightKg(product.getUnitWeightKg())
                    .build();

            orderItemRepository.save(orderItem);
            savedOrder.addOrderItem(orderItem);

            // 11. 재고 감소
            storeProduct.decreaseStock(itemRequest.getQuantity());
        }

        // 12. 저장 후 OrderCreateResponse 반환 (orderId만 포함)
        return OrderCreateResponse.of(savedOrder.getOrderId());
    }

    /**
     * 주문 조회
     * Order 조회 (없으면 ORDER_NOT_FOUND)
     * OrderItem들도 함께 fetch
     * 배송이 할당된 경우 routeId도 함께 조회
     * OrderResponse로 변환하여 반환
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_NOT_FOUND));

        // 배송 경로 ID 조회 (배송이 할당되지 않은 경우 null)
        Long routeId = routeStopOrderRepository.findRouteIdByOrderId(orderId).orElse(null);

        return OrderResponse.from(order, routeId);
    }

    /**
     * 특정 가게의 주문 목록 조회
     * storeId로 가게의 주문들을 조회합니다.
     * status 파라미터가 제공되면 해당 상태의 주문만 필터링합니다.
     * status가 null이면 모든 주문을 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getStoreOrders(Long storeId, OrderStatus status) {
        // Store 존재 확인
        storeRepository.findById(storeId)
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));

        // 주문 조회 (상태별 필터링 또는 전체)
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByStoreStoreIdAndStatus(storeId, status);
        } else {
            orders = orderRepository.findAll().stream()
                    .filter(order -> order.getStore().getStoreId().equals(storeId))
                    .toList();
        }

        // OrderResponse로 변환
        return orders.stream()
                .map(order -> {
                    Long routeId = routeStopOrderRepository.findRouteIdByOrderId(order.getOrderId()).orElse(null);
                    return OrderResponse.from(order, routeId);
                })
                .toList();
    }
}
