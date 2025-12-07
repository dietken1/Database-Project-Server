package backend.databaseproject.domain.route.service;

import backend.databaseproject.domain.order.entity.Order;
import backend.databaseproject.domain.order.repository.OrderRepository;
import backend.databaseproject.domain.route.entity.*;
import backend.databaseproject.domain.route.repository.FlightLogRepository;
import backend.databaseproject.domain.route.repository.RoutePositionRepository;
import backend.databaseproject.domain.route.repository.RouteRepository;
import backend.databaseproject.domain.route.repository.RouteStopRepository;
import backend.databaseproject.global.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 드론 비행 시뮬레이터 서비스
 * 드론의 실시간 위치를 시뮬레이션하고 WebSocket으로 브로드캐스트합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DroneSimulatorService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final RoutePositionRepository routePositionRepository;
    private final FlightLogRepository flightLogRepository;
    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RouteStopProcessingService routeStopProcessingService;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;
    private final backend.databaseproject.domain.drone.repository.DroneRepository droneRepository;

    private static final int UPDATE_INTERVAL_MS = 2000; // 2초마다 업데이트
    private static final double DRONE_SPEED_KMH = 30.0; // 드론 평균 속도 30km/h
    private static final double DRONE_SPEED_MS = DRONE_SPEED_KMH / 3.6; // m/s로 변환
    private static final int INITIAL_BATTERY = 100; // 초기 배터리 100%
    private static final BigDecimal ALTITUDE_M = new BigDecimal("50.00"); // 고도 50m

    // 배터리-거리 변환 상수 (DeliveryBatchService와 동일)
    private static final double BATTERY_TO_DISTANCE_RATIO = 0.004; // mAh당 km (5000mAh = 20km 기준)

    /**
     * 비행 시뮬레이션 시작 (비동기)
     *
     * @param routeId 경로 ID
     */
    @Async
    public void simulateFlight(Long routeId) {
        log.info("드론 비행 시뮬레이션 시작 - RouteId: {}", routeId);

        try {
            // 트랜잭션 커밋 완료를 위한 짧은 대기
            Thread.sleep(100);

            // 1. Route 조회 및 상태 변경 (별도 트랜잭션)
            org.springframework.transaction.support.DefaultTransactionDefinition txDef =
                new org.springframework.transaction.support.DefaultTransactionDefinition();
            txDef.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            org.springframework.transaction.TransactionStatus txStatus = transactionManager.getTransaction(txDef);

            Route route;
            List<RouteStop> stops;
            backend.databaseproject.domain.drone.entity.Drone drone;

            try {
                // 1차 조회: Route, RouteStops, Drone, Store
                route = routeRepository.findByIdWithDetails(routeId)
                        .orElseThrow(() -> new IllegalArgumentException("Route not found: " + routeId));

                stops = route.getRouteStops();
                if (stops.isEmpty()) {
                    log.error("RouteStops가 없습니다 - RouteId: {}", routeId);
                    transactionManager.rollback(txStatus);
                    return;
                }

                // 2차 조회: RouteStopOrders를 fetch (MultipleBagFetchException 회피)
                // RouteStop ID 목록 조회
                List<Long> stopIds = routeRepository.findStopIdsByRouteId(routeId);
                // RouteStopOrders를 한 번에 fetch
                routeStopRepository.findAllWithOrdersByIds(stopIds);

                // Route 상태를 LAUNCHED로 변경
                route.launch();
                routeRepository.saveAndFlush(route);
                drone = route.getDrone();

                transactionManager.commit(txStatus);
                log.info("Route 상태를 LAUNCHED로 변경 - RouteId: {}", routeId);
            } catch (Exception e) {
                transactionManager.rollback(txStatus);
                throw e;
            }

            // Store 정보 추출 (첫 stop에서 가져오기)
            BigDecimal storeLat = stops.get(0).getStopType() == backend.databaseproject.domain.route.entity.StopType.PICKUP ?
                    stops.get(0).getLat() : null;
            BigDecimal storeLng = stops.get(0).getStopType() == backend.databaseproject.domain.route.entity.StopType.PICKUP ?
                    stops.get(0).getLng() : null;

            LocalDateTime flightStartTime = LocalDateTime.now();
            int totalBatteryUsed = 0;
            double totalDistanceTraveled = 0.0;

            // 드론의 배터리 용량으로 배터리 소모율 계산
            double maxDistance = drone.getBatteryCapacity() * BATTERY_TO_DISTANCE_RATIO;
            double batteryDrainRatePerKm = 100.0 / maxDistance; // %/km

            log.info("드론 배터리 정보 - 용량: {}mAh, 최대 거리: {}km, 소모율: {}%/km",
                    drone.getBatteryCapacity(),
                    String.format("%.2f", maxDistance),
                    String.format("%.2f", batteryDrainRatePerKm));

            // 3. RouteStops를 순회하면서 시뮬레이션
            for (int i = 0; i < stops.size(); i++) {
                RouteStop currentStop = stops.get(i);

                BigDecimal startLat;
                BigDecimal startLng;

                // 시작 위치 결정
                if (i == 0) {
                    // 첫 번째 stop은 매장에서 출발
                    startLat = storeLat != null ? storeLat : currentStop.getLat();
                    startLng = storeLng != null ? storeLng : currentStop.getLng();
                } else {
                    // 이전 stop에서 출발
                    RouteStop prevStop = stops.get(i - 1);
                    startLat = prevStop.getLat();
                    startLng = prevStop.getLng();
                }

                BigDecimal endLat = currentStop.getLat();
                BigDecimal endLng = currentStop.getLng();

                // 구간 거리 계산 (km)
                double segmentDistanceKm = GeoUtils.calculateDistance(
                        startLat.doubleValue(), startLng.doubleValue(),
                        endLat.doubleValue(), endLng.doubleValue()
                );
                totalDistanceTraveled += segmentDistanceKm;

                // 예상 이동 시간 계산 (초)
                double segmentTimeSeconds = (segmentDistanceKm * 1000) / DRONE_SPEED_MS;
                int steps = Math.max(1, (int) Math.ceil(segmentTimeSeconds / (UPDATE_INTERVAL_MS / 1000.0)));

                log.info("구간 시뮬레이션 시작 - Stop: {}/{}, 거리: {}km, 단계: {}",
                        i + 1, stops.size(), String.format("%.2f", segmentDistanceKm), steps);

                // 선형 보간으로 이동
                for (int step = 0; step <= steps; step++) {
                    double fraction = (steps > 0) ? (double) step / steps : 0.0;
                    double[] position = GeoUtils.interpolate(
                            startLat.doubleValue(), startLng.doubleValue(),
                            endLat.doubleValue(), endLng.doubleValue(),
                            fraction
                    );

                    // 배터리 소모 계산 (드론별 소모율 적용)
                    double batteryPct = Math.max(0, INITIAL_BATTERY - (totalDistanceTraveled * batteryDrainRatePerKm));

                    // RoutePosition 생성 및 저장 (트랜잭션 없이 직접 저장)
                    Route routeRef = routeRepository.getReferenceById(routeId);
                    RouteStop stopFrom = (i > 0) ? routeStopRepository.getReferenceById(stops.get(i - 1).getStopId()) : null;
                    RouteStop stopTo = routeStopRepository.getReferenceById(currentStop.getStopId());

                    RoutePosition routePosition = RoutePosition.builder()
                            .route(routeRef)
                            .stopFrom(stopFrom)
                            .stopTo(stopTo)
                            .lat(BigDecimal.valueOf(position[0]).setScale(6, RoundingMode.HALF_UP))
                            .lng(BigDecimal.valueOf(position[1]).setScale(6, RoundingMode.HALF_UP))
                            .speedMps(BigDecimal.valueOf(DRONE_SPEED_MS).setScale(2, RoundingMode.HALF_UP))
                            .batteryPct(BigDecimal.valueOf(batteryPct).setScale(2, RoundingMode.HALF_UP))
                            .ts(LocalDateTime.now())
                            .build();

                    routePositionRepository.save(routePosition);

                    // WebSocket으로 브로드캐스트
                    Map<String, Object> positionData = new HashMap<>();
                    positionData.put("routeId", routeId);
                    positionData.put("lat", position[0]);
                    positionData.put("lng", position[1]);
                    positionData.put("speed", DRONE_SPEED_KMH);
                    positionData.put("battery", batteryPct);
                    positionData.put("timestamp", LocalDateTime.now());

                    // Route 구독자에게 전송 (점주용)
                    messagingTemplate.convertAndSend("/topic/route/" + routeId, positionData);

                    // 아직 배송되지 않은 모든 주문들에게 위치 정보 전송 (고객용)
                    // 현재 stop 이후의 모든 DROP stop들의 주문에게 전송 (단, 이미 도착한 stop은 제외)
                    for (int j = i; j < stops.size(); j++) {
                        RouteStop futureStop = stops.get(j);

                        // DROP 타입이고, 아직 도착하지 않은 stop만 처리
                        if (futureStop.getStopType() == StopType.DROP &&
                            futureStop.getStatus() != StopStatus.ARRIVED &&
                            futureStop.getStatus() != StopStatus.DEPARTED) {

                            List<RouteStopOrder> routeStopOrders = futureStop.getRouteStopOrders();
                            for (RouteStopOrder routeStopOrder : routeStopOrders) {
                                Long orderId = routeStopOrder.getOrder().getOrderId();
                                Map<String, Object> customerPositionData = new HashMap<>();
                                customerPositionData.put("orderId", orderId);
                                customerPositionData.put("lat", position[0]);
                                customerPositionData.put("lng", position[1]);
                                customerPositionData.put("speed", DRONE_SPEED_KMH);
                                customerPositionData.put("battery", batteryPct);
                                customerPositionData.put("timestamp", LocalDateTime.now());
                                customerPositionData.put("status", "IN_TRANSIT");

                                messagingTemplate.convertAndSend("/topic/order/" + orderId + "/position", customerPositionData);
                            }
                        }
                    }

                    // 진행 상황 로그 (10% 간격으로만)
                    if (step % Math.max(1, steps / 10) == 0 || step == steps) {
                        log.info("이동 중 - Stop {}/{}, 진행: {}% ({}/{}), 배터리: {}%",
                                i + 1, stops.size(),
                                (int)(fraction * 100), step, steps,
                                String.format("%.1f", batteryPct));
                    }

                    // 2초 대기
                    if (step < steps) {
                        Thread.sleep(UPDATE_INTERVAL_MS);
                    }
                }

                // Stop 도착 처리 (별도 서비스의 별도 트랜잭션으로 즉시 커밋)
                routeStopProcessingService.processStopArrival(currentStop.getStopId());
            }

            // 4. 모든 Stop 완료 후 Route 상태를 COMPLETED로 변경 및 FlightLog 생성 (별도 트랜잭션)
            org.springframework.transaction.support.DefaultTransactionDefinition txDef2 =
                new org.springframework.transaction.support.DefaultTransactionDefinition();
            txDef2.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            org.springframework.transaction.TransactionStatus txStatus2 = transactionManager.getTransaction(txDef2);

            try {
                Route routeToComplete = routeRepository.findById(routeId)
                        .orElseThrow(() -> new IllegalArgumentException("Route not found: " + routeId));

                routeToComplete.complete();
                routeRepository.saveAndFlush(routeToComplete);
                log.info("Route 완료 - RouteId: {}", routeId);

                // 드론 상태를 IDLE로 변경
                backend.databaseproject.domain.drone.entity.Drone droneToUpdate =
                    droneRepository.findById(drone.getDroneId())
                        .orElseThrow(() -> new IllegalArgumentException("Drone not found: " + drone.getDroneId()));
                droneToUpdate.changeStatus(backend.databaseproject.domain.drone.entity.DroneStatus.IDLE);
                droneRepository.saveAndFlush(droneToUpdate);
                log.info("드론 상태 변경 - DroneId: {}, Status: IDLE", droneToUpdate.getDroneId());

                // FlightLog 생성
                LocalDateTime flightEndTime = LocalDateTime.now();
                int batteryUsed = (int) Math.min(INITIAL_BATTERY, totalDistanceTraveled * 5);

                FlightLog flightLog = FlightLog.builder()
                        .route(routeToComplete)
                        .drone(drone)
                        .startTime(flightStartTime)
                        .endTime(flightEndTime)
                        .distance(BigDecimal.valueOf(totalDistanceTraveled).setScale(3, RoundingMode.HALF_UP))
                        .batteryUsed(batteryUsed)
                        .result(FlightResult.SUCCESS)
                        .note("Flight completed successfully")
                        .build();

                flightLogRepository.saveAndFlush(flightLog);
                log.info("FlightLog 생성 완료 - 총 거리: {}km, 배터리 사용: {}%",
                        String.format("%.2f", totalDistanceTraveled), batteryUsed);

                transactionManager.commit(txStatus2);
            } catch (Exception e) {
                transactionManager.rollback(txStatus2);
                throw e;
            }

        } catch (InterruptedException e) {
            log.error("비행 시뮬레이션 중단됨 - RouteId: {}", routeId, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("비행 시뮬레이션 오류 발생 - RouteId: {}", routeId, e);
        }
    }
}
