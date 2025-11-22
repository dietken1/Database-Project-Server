package backend.databaseproject.domain.route.service;

import backend.databaseproject.domain.order.entity.Order;
import backend.databaseproject.domain.store.entity.Store;
import backend.databaseproject.global.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 배송 경로 최적화 서비스
 * TSP (Traveling Salesman Problem) 알고리즘을 구현하여 최적의 배송 경로를 계산합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteOptimizerService {

    /**
     * Nearest Neighbor 휴리스틱을 사용한 경로 최적화
     *
     * @param orders 같은 매장의 배송 요청들
     * @param store    출발 매장
     * @return 최적화된 순서의 배송 요청 리스트
     */
    public List<Order> optimizeRoute(List<Order> orders, Store store) {
        if (orders == null || orders.isEmpty()) {
            log.warn("최적화할 배송 요청이 없습니다.");
            return new ArrayList<>();
        }

        if (orders.size() == 1) {
            log.info("배송 요청이 1개이므로 최적화를 생략합니다.");
            return new ArrayList<>(orders);
        }

        log.info("경로 최적화 시작 - 매장: {}, 배송 요청 수: {}", store.getName(), orders.size());

        List<Order> optimizedRoute = new ArrayList<>();
        Set<Order> unvisited = new HashSet<>(orders);

        // 현재 위치 (매장에서 시작)
        BigDecimal currentLat = store.getLat();
        BigDecimal currentLng = store.getLng();

        // Nearest Neighbor 알고리즘 적용
        while (!unvisited.isEmpty()) {
            Order nearest = null;
            double minDistance = Double.MAX_VALUE;

            // 방문하지 않은 요청 중 가장 가까운 것 찾기
            for (Order order : unvisited) {
                double distance = GeoUtils.calculateDistance(
                        currentLat.doubleValue(),
                        currentLng.doubleValue(),
                        order.getDestLat().doubleValue(),
                        order.getDestLng().doubleValue()
                );

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = order;
                }
            }

            // 가장 가까운 요청을 경로에 추가
            if (nearest != null) {
                optimizedRoute.add(nearest);
                unvisited.remove(nearest);

                // 현재 위치를 방금 추가한 배송지로 업데이트
                currentLat = nearest.getDestLat();
                currentLng = nearest.getDestLng();

                log.debug("다음 배송지 선택 - OrderId: {}, 거리: {:.2f}km",
                         nearest.getOrderId(), minDistance);
            }
        }

        // 총 경로 거리 계산
        double totalDistance = calculateTotalDistance(optimizedRoute, store);
        log.info("경로 최적화 완료 - 총 거리: {:.2f}km, 배송지 수: {}",
                totalDistance, optimizedRoute.size());

        return optimizedRoute;
    }

    /**
     * 최적화된 경로의 총 거리 계산
     */
    private double calculateTotalDistance(List<Order> route, Store store) {
        if (route.isEmpty()) {
            return 0.0;
        }

        double totalDistance = 0.0;
        BigDecimal currentLat = store.getLat();
        BigDecimal currentLng = store.getLng();

        // 매장에서 첫 배송지까지
        for (Order order : route) {
            double distance = GeoUtils.calculateDistance(
                    currentLat.doubleValue(),
                    currentLng.doubleValue(),
                    order.getDestLat().doubleValue(),
                    order.getDestLng().doubleValue()
            );
            totalDistance += distance;
            currentLat = order.getDestLat();
            currentLng = order.getDestLng();
        }

        // 마지막 배송지에서 매장으로 귀환
        double returnDistance = GeoUtils.calculateDistance(
                currentLat.doubleValue(),
                currentLng.doubleValue(),
                store.getLat().doubleValue(),
                store.getLng().doubleValue()
        );
        totalDistance += returnDistance;

        return totalDistance;
    }
}
