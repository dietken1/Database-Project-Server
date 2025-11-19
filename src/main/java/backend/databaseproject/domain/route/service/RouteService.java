package backend.databaseproject.domain.route.service;

import backend.databaseproject.domain.route.dto.response.DronePositionResponse;
import backend.databaseproject.domain.route.dto.response.RouteResponse;
import backend.databaseproject.domain.route.entity.Route;
import backend.databaseproject.domain.route.entity.RoutePosition;
import backend.databaseproject.domain.route.repository.RoutePositionRepository;
import backend.databaseproject.domain.route.repository.RouteRepository;
import backend.databaseproject.global.common.BaseException;
import backend.databaseproject.global.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 배송 경로 서비스
 * 배송 경로 조회 및 드론 위치 조회 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RouteService {

    private final RouteRepository routeRepository;
    private final RoutePositionRepository routePositionRepository;

    /**
     * 경로 상세 조회
     *
     * @param routeId 경로 ID
     * @return 경로 상세 정보
     * @throws BaseException ROUTE_NOT_FOUND
     */
    public RouteResponse getRoute(Long routeId) {
        log.info("경로 조회 - RouteId: {}", routeId);

        Route route = routeRepository.findByIdWithDetails(routeId)
                .orElseThrow(() -> {
                    log.error("경로를 찾을 수 없습니다 - RouteId: {}", routeId);
                    return new BaseException(ErrorCode.ROUTE_NOT_FOUND);
                });

        log.info("경로 조회 완료 - RouteId: {}, Status: {}", routeId, route.getStatus());
        return RouteResponse.from(route);
    }

    /**
     * 드론 현재 위치 조회
     *
     * @param routeId 경로 ID
     * @return 드론 현재 위치 정보
     * @throws BaseException ROUTE_NOT_FOUND, POSITION_NOT_FOUND
     */
    public DronePositionResponse getCurrentPosition(Long routeId) {
        log.info("드론 현재 위치 조회 - RouteId: {}", routeId);

        // Route 존재 여부 확인
        boolean exists = routeRepository.existsById(routeId);
        if (!exists) {
            log.error("경로를 찾을 수 없습니다 - RouteId: {}", routeId);
            throw new BaseException(ErrorCode.ROUTE_NOT_FOUND);
        }

        // 최신 위치 조회
        RoutePosition latestPosition = routePositionRepository.findLatestByRouteId(routeId)
                .orElseThrow(() -> {
                    log.error("위치 정보를 찾을 수 없습니다 - RouteId: {}", routeId);
                    return new BaseException(ErrorCode.POSITION_NOT_FOUND);
                });

        log.info("드론 현재 위치 조회 완료 - RouteId: {}, Battery: {}%",
                routeId, latestPosition.getBattery());

        return DronePositionResponse.from(latestPosition);
    }

    /**
     * 진행 중인 배송 목록 조회
     *
     * @return 진행 중인 배송 경로 목록
     */
    public List<RouteResponse> getActiveRoutes() {
        log.info("진행 중인 배송 목록 조회");

        List<Route> activeRoutes = routeRepository.findActiveRoutesWithStops();

        log.info("진행 중인 배송 목록 조회 완료 - {}건", activeRoutes.size());

        return activeRoutes.stream()
                .map(RouteResponse::from)
                .collect(Collectors.toList());
    }
}
