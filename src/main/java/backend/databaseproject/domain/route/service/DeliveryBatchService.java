package backend.databaseproject.domain.route.service;

import backend.databaseproject.domain.drone.entity.Drone;
import backend.databaseproject.domain.drone.entity.DroneStatus;
import backend.databaseproject.domain.drone.repository.DroneRepository;
import backend.databaseproject.domain.order.entity.DeliveryRequest;
import backend.databaseproject.domain.order.entity.DeliveryStatus;
import backend.databaseproject.domain.order.repository.DeliveryRequestRepository;
import backend.databaseproject.domain.route.entity.*;
import backend.databaseproject.domain.route.repository.RouteRepository;
import backend.databaseproject.domain.route.repository.RouteStopRepository;
import backend.databaseproject.domain.route.repository.RouteStopRequestRepository;
import backend.databaseproject.domain.store.entity.Store;
import backend.databaseproject.domain.store.repository.StoreRepository;
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

    private final DeliveryRequestRepository deliveryRequestRepository;
    private final DroneRepository droneRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final RouteStopRequestRepository routeStopRequestRepository;
    private final StoreRepository storeRepository;
    private final RouteOptimizerService routeOptimizerService;
    private final DroneSimulatorService droneSimulatorService;

    private static final double DRONE_SPEED_KMH = 30.0; // 드론 평균 속도
    private static final int STOP_DELAY_MIN = 2; // 각 stop당 지연 시간 (분)

    /**
     * 배치 처리 실행
     * 대기 중인 배송 요청들을 매장별로 그룹화하여 처리합니다.
     */
    @Transactional
    public void processBatch() {
        log.info("=== 배송 배치 처리 시작 ===");

        try {
            // 1. CREATED 상태의 DeliveryRequest들을 조회
            List<DeliveryRequest> pendingRequests = deliveryRequestRepository
                    .findPendingRequestsWithStoreAndCustomer(DeliveryStatus.CREATED);

            if (pendingRequests.isEmpty()) {
                log.info("처리할 배송 요청이 없습니다.");
                return;
            }

            log.info("대기 중인 배송 요청: {}건", pendingRequests.size());

            // 2. storeId별로 그룹화
            Map<Long, List<DeliveryRequest>> requestsByStore = pendingRequests.stream()
                    .collect(Collectors.groupingBy(req -> req.getStore().getStoreId()));

            log.info("매장별 그룹: {}개 매장", requestsByStore.size());

            // 3. 각 그룹별로 처리
            int processedCount = 0;
            for (Map.Entry<Long, List<DeliveryRequest>> entry : requestsByStore.entrySet()) {
                Long storeId = entry.getKey();
                List<DeliveryRequest> requests = entry.getValue();

                log.info("매장 ID {} 처리 시작 - 배송 요청: {}건", storeId, requests.size());

                // 대기 중인 드론 조회
                Drone availableDrone = droneRepository.findFirstByStatus(DroneStatus.IDLE)
                        .orElse(null);

                if (availableDrone == null) {
                    log.warn("사용 가능한 드론이 없습니다. 매장 ID {} 스킵", storeId);
                    continue;
                }

                log.info("드론 할당 - DroneId: {}, Model: {}",
                        availableDrone.getDroneId(), availableDrone.getModel());

                // 매장 조회
                Store store = storeRepository.findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

                // 경로 최적화
                List<DeliveryRequest> optimizedRequests = routeOptimizerService.optimizeRoute(requests, store);

                if (optimizedRequests.isEmpty()) {
                    log.warn("최적화된 경로가 없습니다. 매장 ID {} 스킵", storeId);
                    continue;
                }

                // Route 생성
                Route route = createRoute(availableDrone, store, optimizedRequests);
                routeRepository.save(route);
                log.info("Route 생성 완료 - RouteId: {}", route.getRouteId());

                // RouteStop 생성
                createRouteStops(route, store, optimizedRequests);

                // DeliveryRequest 상태 변경
                for (DeliveryRequest request : optimizedRequests) {
                    request.assignDelivery();
                }
                deliveryRequestRepository.saveAll(optimizedRequests);

                // 드론 상태 변경
                availableDrone.changeStatus(DroneStatus.IN_FLIGHT);
                droneRepository.save(availableDrone);

                // 비행 시뮬레이션 시작 (비동기)
                droneSimulatorService.simulateFlight(route.getRouteId());

                processedCount += optimizedRequests.size();
                log.info("매장 ID {} 처리 완료 - {}건 배송 할당", storeId, optimizedRequests.size());
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
    private Route createRoute(Drone drone, Store store, List<DeliveryRequest> requests) {
        // 총 거리 계산
        BigDecimal totalDistance = calculateTotalDistance(store, requests);

        // 총 무게 계산
        BigDecimal totalWeight = requests.stream()
                .map(DeliveryRequest::getTotalWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 예상 소요 시간 계산 (거리 / 속도 + 각 stop당 지연 시간)
        double distanceKm = totalDistance.doubleValue();
        double travelTimeHours = distanceKm / DRONE_SPEED_KMH;
        int travelTimeMin = (int) Math.ceil(travelTimeHours * 60);
        int stopDelayMin = (requests.size() + 2) * STOP_DELAY_MIN; // PICKUP + DROP들 + RETURN
        int estimatedDuration = travelTimeMin + stopDelayMin;

        return Route.builder()
                .drone(drone)
                .store(store)
                .totalDistanceKm(totalDistance)
                .totalWeightKg(totalWeight)
                .estimatedDurationMin(estimatedDuration)
                .note("Batch processed at " + java.time.LocalDateTime.now())
                .build();
    }

    /**
     * RouteStop들 생성
     */
    private void createRouteStops(Route route, Store store, List<DeliveryRequest> requests) {
        int sequence = 1;
        BigDecimal currentLat = store.getLat();
        BigDecimal currentLng = store.getLng();

        // 1. PICKUP (매장)
        RouteStop pickupStop = createRouteStop(route, sequence++, StopType.PICKUP,
                store, null, store.getLat(), store.getLng(),
                BigDecimal.ZERO); // 시작점이므로 거리 0
        routeStopRepository.save(pickupStop);

        currentLat = pickupStop.getArrivalLat();
        currentLng = pickupStop.getArrivalLng();

        // 2. DROP들 (각 배송지)
        for (DeliveryRequest request : requests) {
            BigDecimal distance = calculateDistance(currentLat, currentLng,
                    request.getDestLat(), request.getDestLng());

            RouteStop dropStop = createRouteStop(route, sequence++, StopType.DROP,
                    null, request.getCustomer(), request.getDestLat(), request.getDestLng(),
                    distance);
            routeStopRepository.save(dropStop);

            // RouteStopRequest 생성 (Stop과 Request 매핑)
            RouteStopRequest routeStopRequest = RouteStopRequest.builder()
                    .routeStop(dropStop)
                    .deliveryRequest(request)
                    .build();
            routeStopRequestRepository.save(routeStopRequest);

            currentLat = dropStop.getArrivalLat();
            currentLng = dropStop.getArrivalLng();
        }

        // 3. RETURN (매장으로 귀환)
        BigDecimal returnDistance = calculateDistance(currentLat, currentLng,
                store.getLat(), store.getLng());

        RouteStop returnStop = createRouteStop(route, sequence, StopType.RETURN,
                store, null, store.getLat(), store.getLng(),
                returnDistance);
        routeStopRepository.save(returnStop);
    }

    /**
     * RouteStop 생성 헬퍼 메서드
     */
    private RouteStop createRouteStop(Route route, int sequence, StopType type,
                                       Store store, backend.databaseproject.domain.customer.entity.Customer customer,
                                       BigDecimal lat, BigDecimal lng, BigDecimal distanceFromPrev) {
        return RouteStop.builder()
                .route(route)
                .stopSequence(sequence)
                .stopType(type)
                .store(store)
                .customer(customer)
                .arrivalLat(lat)
                .arrivalLng(lng)
                .distanceFromPrevKm(distanceFromPrev)
                .build();
    }

    /**
     * 총 거리 계산 (매장 -> 배송지들 -> 매장)
     */
    private BigDecimal calculateTotalDistance(Store store, List<DeliveryRequest> requests) {
        double totalDistance = 0.0;
        BigDecimal currentLat = store.getLat();
        BigDecimal currentLng = store.getLng();

        // 매장에서 각 배송지까지
        for (DeliveryRequest request : requests) {
            double distance = GeoUtils.calculateDistance(
                    currentLat.doubleValue(), currentLng.doubleValue(),
                    request.getDestLat().doubleValue(), request.getDestLng().doubleValue()
            );
            totalDistance += distance;
            currentLat = request.getDestLat();
            currentLng = request.getDestLng();
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
}
