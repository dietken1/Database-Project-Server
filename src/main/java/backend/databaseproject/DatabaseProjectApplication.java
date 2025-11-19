package backend.databaseproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 드론 배송 관리 시스템 메인 애플리케이션
 *
 * 주요 기능:
 * - 사용자 위치 기반 주변 매장 조회
 * - 카테고리별 상품 조회 및 주문
 * - 10분 단위 배송 배치 처리
 * - 드론 경로 최적화 (TSP 알고리즘)
 * - 실시간 드론 위치 추적 (WebSocket)
 *
 * 접속 주소:
 * - API: http://localhost:8080/api
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - WebSocket: ws://localhost:8080/ws
 */
@SpringBootApplication
@EnableAsync  // 드론 시뮬레이터의 비동기 실행을 위해 필요
public class DatabaseProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseProjectApplication.class, args);
    }

}
