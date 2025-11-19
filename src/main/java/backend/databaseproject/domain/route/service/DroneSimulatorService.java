package backend.databaseproject.domain.route.service;

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
    private final SimpMessagingTemplate messagingTemplate;

    private static final int UPDATE_INTERVAL_MS = 2000; // 2초마다 업데이트
    private static final double DRONE_SPEED_KMH = 30.0; // 드론 평균 속도 30km/h
    private static final double DRONE_SPEED_MS = DRONE_SPEED_KMH / 3.6; // m/s로 변환
    private static final int INITIAL_BATTERY = 100; // 초기 배터리 100%
    private static final BigDecimal ALTITUDE_M = new BigDecimal("50.00"); // 고도 50m

    /**
     * 비행 시뮬레이션 시작 (비동기)
     *
     * @param routeId 경로 ID
     */
    @Async
    @Transactional
    public void simulateFlight(Long routeId) {
        log.info("드론 비행 시뮬레이션 시작 - RouteId: {}", routeId);

        try {
            // 1. Route 조회 (RouteStops 함께 fetch)
            Route route = routeRepository.findByIdWithDetails(routeId)
                    .orElseThrow(() -> new IllegalArgumentException("Route not found: " + routeId));

            List<RouteStop> stops = route.getRouteStops();
            if (stops.isEmpty()) {
                log.error("RouteStops가 없습니다 - RouteId: {}", routeId);
                return;
            }

            // 2. Route 상태를 LAUNCHED로 변경
            route.launch();
            routeRepository.save(route);
            log.info("Route 상태를 LAUNCHED로 변경 - RouteId: {}", routeId);

            LocalDateTime flightStartTime = LocalDateTime.now();
            int totalBatteryUsed = 0;
            double totalDistanceTraveled = 0.0;

            // 3. RouteStops를 순회하면서 시뮬레이션
            for (int i = 0; i < stops.size(); i++) {
                RouteStop currentStop = stops.get(i);

                BigDecimal startLat;
                BigDecimal startLng;

                // 시작 위치 결정
                if (i == 0) {
                    // 첫 번째 stop은 매장에서 출발
                    startLat = route.getStore().getLat();
                    startLng = route.getStore().getLng();
                } else {
                    // 이전 stop에서 출발
                    RouteStop prevStop = stops.get(i - 1);
                    startLat = prevStop.getArrivalLat();
                    startLng = prevStop.getArrivalLng();
                }

                BigDecimal endLat = currentStop.getArrivalLat();
                BigDecimal endLng = currentStop.getArrivalLng();

                // 구간 거리 계산 (km)
                double segmentDistanceKm = GeoUtils.calculateDistance(
                        startLat.doubleValue(), startLng.doubleValue(),
                        endLat.doubleValue(), endLng.doubleValue()
                );
                totalDistanceTraveled += segmentDistanceKm;

                // 예상 이동 시간 계산 (초)
                double segmentTimeSeconds = (segmentDistanceKm * 1000) / DRONE_SPEED_MS;
                int steps = (int) Math.ceil(segmentTimeSeconds / (UPDATE_INTERVAL_MS / 1000.0));

                log.info("구간 시뮬레이션 시작 - Stop: {}/{}, 거리: {:.2f}km, 단계: {}",
                        i + 1, stops.size(), segmentDistanceKm, steps);

                // 선형 보간으로 이동
                for (int step = 0; step <= steps; step++) {
                    double fraction = (double) step / steps;
                    double[] position = GeoUtils.interpolate(
                            startLat.doubleValue(), startLng.doubleValue(),
                            endLat.doubleValue(), endLng.doubleValue(),
                            fraction
                    );

                    // 배터리 소모 계산 (거리에 비례)
                    int battery = Math.max(0, INITIAL_BATTERY - (int) (totalDistanceTraveled * 5));

                    // RoutePosition 생성 및 저장
                    RoutePosition routePosition = RoutePosition.builder()
                            .route(route)
                            .lat(BigDecimal.valueOf(position[0]).setScale(6, RoundingMode.HALF_UP))
                            .lng(BigDecimal.valueOf(position[1]).setScale(6, RoundingMode.HALF_UP))
                            .altitudeM(ALTITUDE_M)
                            .speedKmh(BigDecimal.valueOf(DRONE_SPEED_KMH).setScale(2, RoundingMode.HALF_UP))
                            .battery(battery)
                            .recordedAt(LocalDateTime.now())
                            .build();

                    routePositionRepository.save(routePosition);

                    // WebSocket으로 브로드캐스트
                    Map<String, Object> positionData = new HashMap<>();
                    positionData.put("routeId", routeId);
                    positionData.put("lat", position[0]);
                    positionData.put("lng", position[1]);
                    positionData.put("altitude", ALTITUDE_M);
                    positionData.put("speed", DRONE_SPEED_KMH);
                    positionData.put("battery", battery);
                    positionData.put("timestamp", LocalDateTime.now());

                    messagingTemplate.convertAndSend("/topic/route/" + routeId, positionData);

                    // 2초 대기
                    if (step < steps) {
                        Thread.sleep(UPDATE_INTERVAL_MS);
                    }
                }

                // Stop 도착 처리
                currentStop.arrive();
                routeStopRepository.save(currentStop);
                log.info("Stop 도착 - StopId: {}, Type: {}", currentStop.getStopId(), currentStop.getStopType());

                // DROP 타입은 잠시 대기 (배송 시뮬레이션)
                if (currentStop.getStopType() == StopType.DROP) {
                    Thread.sleep(3000); // 3초 대기
                    currentStop.depart();
                    routeStopRepository.save(currentStop);
                }
            }

            // 4. 모든 Stop 완료 후 Route 상태를 COMPLETED로 변경
            route.complete();
            routeRepository.save(route);
            log.info("Route 완료 - RouteId: {}", routeId);

            // 5. FlightLog 생성
            LocalDateTime flightEndTime = LocalDateTime.now();
            Duration flightDuration = Duration.between(flightStartTime, flightEndTime);
            int durationMinutes = (int) flightDuration.toMinutes();

            FlightLog flightLog = FlightLog.builder()
                    .route(route)
                    .drone(route.getDrone())
                    .startTime(flightStartTime)
                    .endTime(flightEndTime)
                    .distanceKm(BigDecimal.valueOf(totalDistanceTraveled).setScale(2, RoundingMode.HALF_UP))
                    .durationMin(durationMinutes)
                    .maxAltitudeM(ALTITUDE_M)
                    .avgSpeedKmh(BigDecimal.valueOf(DRONE_SPEED_KMH).setScale(2, RoundingMode.HALF_UP))
                    .batteryUsed(INITIAL_BATTERY - Math.max(0, INITIAL_BATTERY - (int) (totalDistanceTraveled * 5)))
                    .result(FlightResult.SUCCESS)
                    .build();

            flightLogRepository.save(flightLog);
            log.info("FlightLog 생성 완료 - 총 거리: {:.2f}km, 소요 시간: {}분",
                    totalDistanceTraveled, durationMinutes);

        } catch (InterruptedException e) {
            log.error("비행 시뮬레이션 중단됨 - RouteId: {}", routeId, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("비행 시뮬레이션 오류 발생 - RouteId: {}", routeId, e);
        }
    }
}
