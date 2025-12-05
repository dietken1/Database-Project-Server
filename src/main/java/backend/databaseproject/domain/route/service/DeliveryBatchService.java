package backend.databaseproject.domain.route.service;

import backend.databaseproject.domain.drone.entity.Drone;
import backend.databaseproject.domain.drone.entity.DroneStatus;
import backend.databaseproject.domain.drone.repository.DroneRepository;
import backend.databaseproject.domain.order.entity.Order;
import backend.databaseproject.domain.order.entity.OrderStatus;
import backend.databaseproject.domain.order.repository.OrderRepository;
import backend.databaseproject.domain.route.entity.*;
import backend.databaseproject.domain.route.repository.RouteRepository;
import backend.databaseproject.domain.route.repository.RouteStopRepository;
import backend.databaseproject.domain.route.repository.RouteStopOrderRepository;
import backend.databaseproject.domain.store.entity.Store;
import backend.databaseproject.domain.store.repository.StoreRepository;
import backend.databaseproject.global.exception.BatteryInsufficientException;
import backend.databaseproject.global.exception.PayloadExceededException;
import backend.databaseproject.global.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 배송 배치 처리 서비스
 * 대기 중인 주문들을 주기적으로 처리하여 배송 경로를 생성합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryBatchService {

    private final OrderRepository orderRepository;
    private final DroneRepository droneRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final RouteStopOrderRepository routeStopOrderRepository;
    private final StoreRepository storeRepository;
    private final RouteOptimizerService routeOptimizerService;
    private final DroneSimulatorService droneSimulatorService;

    private static final double DRONE_SPEED_KMH = 30.0; // 드론 평균 속도
    private static final int STOP_DELAY_MIN = 2; // 각 stop당 지연 시간 (분)

    // 배터리-거리 변환 상수
    private static final double BATTERY_TO_DISTANCE_RATIO = 0.004; // mAh당 km (5000mAh = 20km 기준)
    private static final double SAFETY_MARGIN = 0.8; // 안전 마진 (80% 사용, 20% 여유)

    /**
     * 선택된 주문들로 배송 시작
     * 점주가 선택한 주문 ID들을 받아 배송을 시작합니다.
     *
     * @param orderIds 배송할 주문 ID 리스트 (최대 3개)
     * @throws IllegalArgumentException 주문을 찾을 수 없거나, 상태가 CREATED가 아니거나, 매장이 다른 경우
     * @throws PayloadExceededException 드론의 적재량을 초과한 경우
     * @throws BatteryInsufficientException 배터리 용량이 부족한 경우
     */
    @Transactional
    public void processSelectedOrders(List<Long> orderIds) {
        log.info("=== 선택된 주문 배송 시작 ===");
        log.info("요청된 주문 ID: {}", orderIds);

        // 1. 주문 조회
        List<Order> orders = new ArrayList<>();
        for (Long orderId : orderIds) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new backend.databaseproject.domain.order.exception.OrderNotFoundException(
                            String.format("주문 ID %d를 찾을 수 없습니다.", orderId)));
            orders.add(order);
        }

        // 2. 모든 주문이 CREATED 상태인지 확인
        for (Order order : orders) {
            if (order.getStatus() != OrderStatus.CREATED) {
                String statusMessage = getOrderStatusMessage(order.getStatus());
                throw new backend.databaseproject.domain.order.exception.OrderAlreadyProcessedException(
                        String.format("주문 ID %d는 %s 상태입니다. 배송을 시작할 수 없습니다.",
                                order.getOrderId(), statusMessage));
            }
        }

        // 3. 모든 주문이 같은 매장인지 확인
        Store store = orders.get(0).getStore();
        for (Order order : orders) {
            if (!order.getStore().getStoreId().equals(store.getStoreId())) {
                throw new IllegalArgumentException(
                        String.format("모든 주문은 같은 매장이어야 합니다. 매장 ID: %d vs %d",
                                store.getStoreId(), order.getStore().getStoreId()));
            }
        }

        log.info("매장: {}, 주문 수: {}", store.getName(), orders.size());

        // 4. 사용 가능한 드론 조회
        Drone availableDrone = droneRepository.findFirstByStoreAndStatus(store, DroneStatus.IDLE)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("매장 '%s'에 사용 가능한 드론이 없습니다.", store.getName())));

        log.info("드론 할당 - DroneId: {}, Model: {}, MaxPayload: {}kg",
                availableDrone.getDroneId(), availableDrone.getModel(), availableDrone.getMaxPayloadKg());

        // 5. 무게 및 거리 검증
        validatePayloadAndDistance(orders, availableDrone, store);

        // 6. 주문 시간순 정렬
        orders.sort((o1, o2) -> o1.getCreatedAt().compareTo(o2.getCreatedAt()));

        // 7. 경로 최적화
        List<Order> optimizedOrders = routeOptimizerService.optimizeRoute(orders, store);

        // 8. Route 생성
        Route route = createRoute(availableDrone, store, optimizedOrders);
        routeRepository.save(route);
        log.info("Route 생성 완료 - RouteId: {}", route.getRouteId());

        // 9. RouteStop 생성
        createRouteStops(route, store, optimizedOrders);

        // 10. Order 상태 변경
        for (Order order : optimizedOrders) {
            order.assignDelivery();
        }
        orderRepository.saveAll(optimizedOrders);

        // 11. 드론 상태 변경
        availableDrone.changeStatus(DroneStatus.IN_FLIGHT);
        droneRepository.save(availableDrone);

        // 12. 비행 시뮬레이션 시작 (비동기)
        droneSimulatorService.simulateFlight(route.getRouteId());

        log.info("=== 선택된 주문 배송 시작 완료 - RouteId: {} ===", route.getRouteId());
    }

    /**
     * 배치 처리 실행
     * 대기 중인 배송 요청들을 매장별로 그룹화하여 처리합니다.
     */
    @Transactional
    public void processBatch() {
        log.info("=== 배송 배치 처리 시작 ===");

        try {
            // 1. CREATED 상태의 Order들을 조회 (주문 시간순 정렬)
            List<Order> pendingOrders = orderRepository
                    .findPendingOrdersWithStoreAndUser(OrderStatus.CREATED);

            if (pendingOrders.isEmpty()) {
                log.info("처리할 배송 요청이 없습니다.");
                return;
            }

            // 주문 시간순으로 정렬 (먼저 주문한 고객 우선)
            pendingOrders.sort((o1, o2) -> o1.getCreatedAt().compareTo(o2.getCreatedAt()));

            log.info("대기 중인 배송 요청: {}건", pendingOrders.size());

            // 2. storeId별로 그룹화
            Map<Long, List<Order>> ordersByStore = pendingOrders.stream()
                    .collect(Collectors.groupingBy(order -> order.getStore().getStoreId()));

            log.info("매장별 그룹: {}개 매장", ordersByStore.size());

            // 3. 각 그룹별로 처리
            int processedCount = 0;
            for (Map.Entry<Long, List<Order>> entry : ordersByStore.entrySet()) {
                Long storeId = entry.getKey();
                List<Order> orders = entry.getValue();

                log.info("매장 ID {} 처리 시작 - 배송 요청: {}건", storeId, orders.size());

                // 매장 조회
                Store store = storeRepository.findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

                // 해당 매장의 대기 중인 드론 조회
                Drone availableDrone = droneRepository.findFirstByStoreAndStatus(store, DroneStatus.IDLE)
                        .orElse(null);

                if (availableDrone == null) {
                    log.warn("매장 ID {}에 사용 가능한 드론이 없습니다. 스킵", storeId);
                    continue;
                }

                log.info("드론 할당 - DroneId: {}, Model: {}, MaxPayload: {}kg, Store: {}",
                        availableDrone.getDroneId(), availableDrone.getModel(),
                        availableDrone.getMaxPayloadKg(), store.getName());

                // 드론의 적재량과 배터리를 고려하여 할당 가능한 주문 선택
                List<Order> selectedOrders = selectOrdersForDrone(orders, availableDrone, store);

                if (selectedOrders.isEmpty()) {
                    log.warn("드론에 할당 가능한 주문이 없습니다. 매장 ID {} 스킵", storeId);
                    continue;
                }

                log.info("할당 가능한 주문: {}건 / 전체 {}건", selectedOrders.size(), orders.size());

                // 경로 최적화
                List<Order> optimizedOrders = routeOptimizerService.optimizeRoute(selectedOrders, store);

                if (optimizedOrders.isEmpty()) {
                    log.warn("최적화된 경로가 없습니다. 매장 ID {} 스킵", storeId);
                    continue;
                }

                // Route 생성
                Route route = createRoute(availableDrone, store, optimizedOrders);
                routeRepository.save(route);
                log.info("Route 생성 완료 - RouteId: {}", route.getRouteId());

                // RouteStop 생성
                createRouteStops(route, store, optimizedOrders);

                // Order 상태 변경
                for (Order order : optimizedOrders) {
                    order.assignDelivery();
                }
                orderRepository.saveAll(optimizedOrders);

                // 드론 상태 변경
                availableDrone.changeStatus(DroneStatus.IN_FLIGHT);
                droneRepository.save(availableDrone);

                // 트랜잭션 커밋 후 비행 시뮬레이션 시작 (비동기)
                Long routeIdForSimulation = route.getRouteId();
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                droneSimulatorService.simulateFlight(routeIdForSimulation);
                            }
                        }
                );

                processedCount += optimizedOrders.size();
                log.info("매장 ID {} 처리 완료 - {}건 배송 할당", storeId, optimizedOrders.size());
            }

            log.info("=== 배송 배치 처리 완료 - 총 {}건 처리 ===", processedCount);

        } catch (Exception e) {
            log.error("배송 배치 처리 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * Route 엔티티 생성
     */
    private Route createRoute(Drone drone, Store store, List<Order> orders) {
        // 총 거리 계산
        BigDecimal totalDistance = calculateTotalDistance(store, orders);

        // 총 무게 계산
        BigDecimal totalWeight = orders.stream()
                .map(Order::getTotalWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 예상 소요 시간 계산 (거리 / 속도 + 각 stop당 지연 시간)
        double distanceKm = totalDistance.doubleValue();
        double travelTimeHours = distanceKm / DRONE_SPEED_KMH;
        int travelTimeMin = (int) Math.ceil(travelTimeHours * 60);
        int stopDelayMin = (orders.size() + 2) * STOP_DELAY_MIN; // PICKUP + DROP들 + RETURN
        int estimatedDuration = travelTimeMin + stopDelayMin;

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        return Route.builder()
                .drone(drone)
                .store(store)
                .plannedTotalDistanceKm(totalDistance)
                .plannedTotalPayloadKg(totalWeight)
                .plannedStartAt(now)
                .plannedEndAt(now.plusMinutes(estimatedDuration))
                .heuristic("Nearest Neighbor")
                .note("Batch processed at " + now)
                .build();
    }

    /**
     * RouteStop들 생성
     */
    private void createRouteStops(Route route, Store store, List<Order> orders) {
        int sequence = 1;

        // 1. PICKUP (매장)
        RouteStop pickupStop = createRouteStop(route, sequence++, StopType.PICKUP,
                store.getName(), store, null, store.getLat(), store.getLng(), null);
        routeStopRepository.save(pickupStop);

        // 2. DROP들 (각 배송지)
        for (Order order : orders) {
            RouteStop dropStop = createRouteStop(route, sequence++, StopType.DROP,
                    order.getUser().getName(), null, order.getUser(),
                    order.getDestLat(), order.getDestLng(),
                    order.getTotalWeightKg().negate()); // 배송으로 무게 감소
            routeStopRepository.save(dropStop);

            // RouteStopOrder 생성 (Stop과 Order 매핑)
            RouteStopOrder routeStopOrder = RouteStopOrder.builder()
                    .routeStop(dropStop)
                    .order(order)
                    .build();
            routeStopOrderRepository.save(routeStopOrder);
        }

        // 3. RETURN (매장으로 귀환)
        RouteStop returnStop = createRouteStop(route, sequence, StopType.RETURN,
                store.getName(), store, null, store.getLat(), store.getLng(), null);
        routeStopRepository.save(returnStop);
    }

    /**
     * RouteStop 생성 헬퍼 메서드
     */
    private RouteStop createRouteStop(Route route, int sequence, StopType type, String name,
                                       Store store, backend.databaseproject.domain.user.entity.User user,
                                       BigDecimal lat, BigDecimal lng, BigDecimal payloadDelta) {
        return RouteStop.builder()
                .route(route)
                .stopSequence(sequence)
                .stopType(type)
                .name(name)
                .lat(lat)
                .lng(lng)
                .payloadDeltaKg(payloadDelta)
                .store(store)
                .user(user)
                .build();
    }

    /**
     * 총 거리 계산 (매장 -> 배송지들 -> 매장)
     */
    private BigDecimal calculateTotalDistance(Store store, List<Order> orders) {
        double totalDistance = 0.0;
        BigDecimal currentLat = store.getLat();
        BigDecimal currentLng = store.getLng();

        // 매장에서 각 배송지까지
        for (Order order : orders) {
            double distance = GeoUtils.calculateDistance(
                    currentLat.doubleValue(), currentLng.doubleValue(),
                    order.getDestLat().doubleValue(), order.getDestLng().doubleValue()
            );
            totalDistance += distance;
            currentLat = order.getDestLat();
            currentLng = order.getDestLng();
        }

        // 마지막 배송지에서 매장으로 귀환
        double returnDistance = GeoUtils.calculateDistance(
                currentLat.doubleValue(), currentLng.doubleValue(),
                store.getLat().doubleValue(), store.getLng().doubleValue()
        );
        totalDistance += returnDistance;

        return BigDecimal.valueOf(totalDistance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 두 지점 간 거리 계산
     */
    private BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lng1,
                                          BigDecimal lat2, BigDecimal lng2) {
        double distance = GeoUtils.calculateDistance(
                lat1.doubleValue(), lng1.doubleValue(),
                lat2.doubleValue(), lng2.doubleValue()
        );
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 드론의 적재량과 배터리를 고려하여 할당 가능한 주문 선택
     * 주문 시간순으로 처리하되, 드론의 물리적 제약을 초과하지 않는 주문들만 선택합니다.
     *
     * @param orders 같은 매장의 대기 중인 주문들 (이미 시간순 정렬됨)
     * @param drone 할당할 드론
     * @param store 출발 매장
     * @return 할당 가능한 주문 리스트
     */
    private List<Order> selectOrdersForDrone(List<Order> orders, Drone drone, Store store) {
        List<Order> selectedOrders = new ArrayList<>();
        BigDecimal totalWeight = BigDecimal.ZERO;
        double totalDistance = 0.0;

        BigDecimal currentLat = store.getLat();
        BigDecimal currentLng = store.getLng();

        // 드론의 배터리 용량으로 최대 거리 계산
        double maxDistance = calculateMaxDistance(drone);

        log.info("주문 선택 시작 - 드론 최대 적재량: {}kg, 최대 거리: {}km (배터리: {}mAh)",
                drone.getMaxPayloadKg(), String.format("%.2f", maxDistance), drone.getBatteryCapacity());

        for (Order order : orders) {
            // 1. 적재량 체크
            BigDecimal newTotalWeight = totalWeight.add(order.getTotalWeightKg());
            if (newTotalWeight.compareTo(drone.getMaxPayloadKg()) > 0) {
                log.info("적재량 초과로 주문 스킵 - OrderId: {}, 현재 무게: {}kg, 주문 무게: {}kg, 최대: {}kg",
                        order.getOrderId(), totalWeight, order.getTotalWeightKg(), drone.getMaxPayloadKg());
                continue; // 적재량 초과, 다음 주문 확인
            }

            // 2. 거리 체크 (현재 위치 -> 배송지 -> 매장 귀환 거리 계산)
            double distanceToOrder = GeoUtils.calculateDistance(
                    currentLat.doubleValue(), currentLng.doubleValue(),
                    order.getDestLat().doubleValue(), order.getDestLng().doubleValue()
            );

            double distanceBackToStore = GeoUtils.calculateDistance(
                    order.getDestLat().doubleValue(), order.getDestLng().doubleValue(),
                    store.getLat().doubleValue(), store.getLng().doubleValue()
            );

            double newTotalDistance = totalDistance + distanceToOrder + distanceBackToStore;

            // 현재까지의 거리에서 귀환 거리를 빼고 새로운 경로를 추가
            if (!selectedOrders.isEmpty()) {
                // 이전 귀환 거리 제거
                double prevReturnDistance = GeoUtils.calculateDistance(
                        currentLat.doubleValue(), currentLng.doubleValue(),
                        store.getLat().doubleValue(), store.getLng().doubleValue()
                );
                newTotalDistance = totalDistance - prevReturnDistance + distanceToOrder + distanceBackToStore;
            }

            if (newTotalDistance > maxDistance) {
                log.info("거리 초과로 주문 스킵 - OrderId: {}, 예상 총 거리: {}km, 최대: {}km",
                        order.getOrderId(), String.format("%.2f", newTotalDistance), String.format("%.2f", maxDistance));
                continue; // 거리 초과, 다음 주문 확인
            }

            // 3. 제약 조건 통과 - 주문 추가
            selectedOrders.add(order);
            totalWeight = newTotalWeight;
            totalDistance = newTotalDistance;
            currentLat = order.getDestLat();
            currentLng = order.getDestLng();

            log.info("주문 선택 - OrderId: {}, 누적 무게: {}kg, 예상 거리: {}km",
                    order.getOrderId(), totalWeight, String.format("%.2f", totalDistance));
        }

        log.info("주문 선택 완료 - 선택: {}건, 총 무게: {}kg, 예상 거리: {}km",
                selectedOrders.size(), totalWeight, String.format("%.2f", totalDistance));

        return selectedOrders;
    }

    /**
     * 무게 및 거리 검증
     * 주문들의 총 무게와 예상 거리가 드론의 제한을 초과하는지 확인합니다.
     *
     * @param orders 검증할 주문 리스트
     * @param drone 할당된 드론
     * @param store 출발 매장
     * @throws PayloadExceededException 적재량 초과 시
     * @throws BatteryInsufficientException 배터리 용량 부족 시
     */
    private void validatePayloadAndDistance(List<Order> orders, Drone drone, Store store) {
        // 1. 총 무게 계산
        BigDecimal totalWeight = orders.stream()
                .map(Order::getTotalWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("총 무게: {}kg / 최대 적재량: {}kg", totalWeight, drone.getMaxPayloadKg());

        if (totalWeight.compareTo(drone.getMaxPayloadKg()) > 0) {
            throw new PayloadExceededException(
                    String.format("드론의 최대 적재량을 초과했습니다. 총 무게: %skg, 최대 적재량: %skg",
                            totalWeight, drone.getMaxPayloadKg()));
        }

        // 2. 드론의 배터리 용량으로 최대 거리 계산
        double maxDistance = calculateMaxDistance(drone);

        // 3. 예상 거리 계산 (매장 -> 각 배송지 -> 매장)
        double totalDistance = 0.0;
        BigDecimal currentLat = store.getLat();
        BigDecimal currentLng = store.getLng();

        // 매장에서 각 배송지까지
        for (Order order : orders) {
            double distance = GeoUtils.calculateDistance(
                    currentLat.doubleValue(), currentLng.doubleValue(),
                    order.getDestLat().doubleValue(), order.getDestLng().doubleValue()
            );
            totalDistance += distance;
            currentLat = order.getDestLat();
            currentLng = order.getDestLng();
        }

        // 마지막 배송지에서 매장으로 귀환
        double returnDistance = GeoUtils.calculateDistance(
                currentLat.doubleValue(), currentLng.doubleValue(),
                store.getLat().doubleValue(), store.getLng().doubleValue()
        );
        totalDistance += returnDistance;

        log.info("예상 총 거리: {}km / 최대 거리: {}km (배터리: {}mAh)",
                String.format("%.2f", totalDistance), String.format("%.2f", maxDistance), drone.getBatteryCapacity());

        if (totalDistance > maxDistance) {
            throw new BatteryInsufficientException(
                    String.format("배터리 용량이 부족합니다. 예상 거리: %.2fkm, 최대 거리: %.2fkm (배터리: %dmAh)",
                            totalDistance, maxDistance, drone.getBatteryCapacity()));
        }

        log.info("무게 및 거리 검증 통과");
    }

    /**
     * 드론의 배터리 용량을 기반으로 최대 비행 가능 거리 계산
     *
     * @param drone 드론
     * @return 최대 비행 가능 거리 (km) - 안전 마진 포함
     */
    private double calculateMaxDistance(Drone drone) {
        // 배터리 용량(mAh)을 최대 거리(km)로 변환
        double maxDistance = drone.getBatteryCapacity() * BATTERY_TO_DISTANCE_RATIO;

        // 안전 마진 적용 (80% 사용, 20% 여유)
        double safeDistance = maxDistance * SAFETY_MARGIN;

        log.debug("드론 ID {}: 배터리 {}mAh, 최대 거리 {}km, 안전 거리 {}km",
                drone.getDroneId(), drone.getBatteryCapacity(), String.format("%.2f", maxDistance), String.format("%.2f", safeDistance));

        return safeDistance;
    }

    /**
     * 주문 상태에 따른 한글 메시지 반환
     *
     * @param status 주문 상태
     * @return 한글 메시지
     */
    private String getOrderStatusMessage(OrderStatus status) {
        return switch (status) {
            case CREATED -> "주문 생성됨";
            case ASSIGNED -> "이미 배송이 시작됨";
            case FULFILLED -> "이미 배송이 완료됨";
            case CANCELED -> "취소된 주문";
            case FAILED -> "배송 실패";
        };
    }
}
