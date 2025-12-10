# 드론 멀티배송 시스템 (Drone Multi-Delivery System)

경로 최적화와 실시간 추적 기능을 갖춘 드론 멀티 배송 시스템

## 📋 프로젝트 개요

최근 이커머스 시장의 폭발적인 성장으로 근거리 배송 수요가 급증하고 있습니다. 하지만 기존 운송 수단은 환경 오염, 인력 부족, 그리고 도심 교통 혼잡이라는 한계에 부딪혔습니다.

본 프로젝트는 이러한 문제를 해결하기 위한 드론 멀티 배송 시스템 프로토타입으로, 여러 주문을 하나의 비행 임무로 묶는 경로 최적화 로직과 배터리 및 적재 중량 제약을 고려한 현실적인 모델링, 그리고 주문부터 배송까지 전 과정을 추적하는 데이터베이스 구조를 구현했습니다.

### 핵심 기능

- **멀티 목적지 경로 최적화**: TSP 알고리즘 기반, 물품 하차에 따른 탑재 중량 변화 고려
- **실시간 위치 추적**: WebSocket 기반 2초 단위 위치 업데이트
- **데이터베이스 중심 설계**: 주문-배송-로그 전 과정 추적 가능
- **배송 이력 보존**: 스냅샷 데이터 저장으로 과거 배송 경로 재현 가능

## 🎯 프로젝트 목표

1. **환경 친화적 배송**: 탄소 배출 감축을 위한 전기 드론 활용
2. **배송 효율화**: 교통 체증 회피 및 인력 부족 해소
3. **경로 최적화**: 여러 배송지를 효율적으로 묶어 비행 거리 최소화
4. **실시간 모니터링**: 배송 과정 전체를 실시간으로 추적 및 관리

## 🛠 기술 스택

### 백엔드
- **Java 17** - 최신 LTS 버전
- **Spring Boot 3.5.7** - 백엔드 프레임워크
- **Spring Data JPA** - ORM 및 데이터베이스 접근
- **MySQL 8.x** - 관계형 데이터베이스
- **WebSocket (STOMP)** - 실시간 양방향 통신

### 프론트엔드
- **React** - UI 프레임워크
- **Kakao Map API** - 지도 시각화
- **SockJS + STOMP** - WebSocket 클라이언트

### 개발 도구
- **Gradle** - 빌드 도구
- **Swagger (SpringDoc OpenAPI)** - API 문서화
- **Lombok** - 보일러플레이트 코드 감소

## 🏗 시스템 아키텍처

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│                 │         │                 │         │                 │
│  React Client   │◄───────►│  Spring Boot    │◄───────►│     MySQL       │
│  (고객/점주 UI)  │  REST   │   Backend       │  JDBC   │   Database      │
│                 │ WebSocket│                 │         │                 │
└─────────────────┘         └─────────────────┘         └─────────────────┘
        │                           │
        │                           │
        └───────────┬───────────────┘
                    │
            ┌───────▼────────┐
            │                │
            │ Drone Simulator│
            │   (백엔드 내장)  │
            │                │
            └────────────────┘
```

## 📊 데이터베이스 설계

### 주요 테이블 구조

#### 1. 매장 & 상품 도메인
- `store`: 매장 정보 (위치, 배송 반경)
- `product`: 상품 정보 (무게, 카테고리)
- `store_product`: 매장별 상품 재고 및 가격

#### 2. 사용자 & 주문 도메인
- `user`: 사용자 정보 (CUSTOMER, OWNER 역할)
- `orders`: 주문 정보 (배송지 좌표 스냅샷 저장)
- `order_item`: 주문 상세 품목

#### 3. 드론 & 배송 도메인
- `drone`: 드론 기체 정보 (배터리 용량, 최대 적재량)
- `route`: 배송 임무 (계획/실제 거리, 시작/종료 시각)
- `route_stop`: 경유지 정보 (순서, 상태, 도착/출발 시각)
- `route_stop_order`: 경유지별 주문 매핑 (멀티배송 지원)

#### 4. 로그 & 추적 도메인
- `route_position`: 2초 단위 드론 위치 기록
- `flight_log`: 비행 결과 요약 (거리, 배터리 사용량)

### 멀티배송 모델링

```sql
-- 하나의 Route에 여러 RouteStop이 연결
route (1) ──< (N) route_stop

-- 하나의 RouteStop에서 여러 Order 처리 가능
route_stop (1) ──< (N) route_stop_order >── (1) orders
```

### 정규화 전략

- **3NF 준수**: 주문 헤더/상세, 임무/로그 분리로 중복 최소화
- **의도적 반정규화**: 주문 시점 좌표를 `orders` 테이블에 저장하여 과거 배송 경로 재현 가능
- **CASCADE 설정**: 부모 엔티티 삭제 시 자식 데이터 자동 삭제로 데이터 무결성 보장

## 🚀 핵심 기능 상세

### 1. 경로 최적화 (TSP 알고리즘)

**Nearest Neighbor 휴리스틱** 사용:
- 매장에서 출발하여 가장 가까운 배송지부터 순회
- Haversine 공식으로 실제 지구 곡률 고려한 거리 계산
- 물품 하차에 따른 무게 감소 반영

```
매장 → 배송지1 (최단거리) → 배송지2 (현재 위치에서 최단) → ... → 매장
```

### 2. 드론 시뮬레이터

실제 드론 없이 백엔드 내부에서 비행 시뮬레이션:

- **비동기 처리**: `@Async`로 별도 스레드에서 실행
- **2초 단위 업데이트**: 선형 보간으로 부드러운 이동 구현
- **배터리 소모 계산**: 거리와 탑재 중량에 비례한 배터리 감소
- **실시간 DB 저장**: `route_position` 테이블에 전체 경로 기록

### 3. WebSocket 실시간 통신

**점주용 추적**:
```javascript
// Route ID로 전체 경로 추적
stompClient.subscribe('/topic/route/{routeId}', callback);
```

**고객용 추적**:
```javascript
// Order ID로 자신의 주문만 추적
stompClient.subscribe('/topic/order/{orderId}/position', callback);
```

### 4. 트랜잭션 관리

각 경유지마다 독립적인 트랜잭션으로 **실시간 상태 업데이트**:

```java
// 메인 시뮬레이션 (전체 경로 관리)
@Transactional(REQUIRES_NEW)
simulateFlight()

// 경유지별 처리 (즉시 커밋)
RouteStopProcessingService.processStopArrival()
  ├─> RouteStop 상태 업데이트 (ARRIVED → DEPARTED)
  ├─> Order 상태 업데이트 (FULFILLED)
  └─> flush() - 즉시 DB 반영
```

## 📡 주요 API 엔드포인트

### 매장 조회
```http
GET /api/stores?lat=37.287234&lng=127.046789&radius=3.0
```

### 주문 생성
```http
POST /api/orders
Content-Type: application/json

{
  "storeId": 1,
  "userId": 1,
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ]
}
```

### 배송 시작 (점주용)
```http
POST /api/routes/start-delivery
Content-Type: application/json

{
  "orderIds": [1, 2, 3]
}
```

### 드론 위치 조회
```http
GET /api/routes/{routeId}/current-position
```

## 🔑 핵심 SQL 쿼리

### 1. 배송 대기 주문 조회
```sql
SELECT o.*
FROM orders o
WHERE o.store_id = ?
  AND o.status = 'CREATED'
ORDER BY o.created_at ASC;
```

### 2. 경로 정류장 순서대로 조회
```sql
SELECT * FROM route_stop
WHERE route_id = ?
ORDER BY stop_sequence ASC;
```

### 3. 배송 완료 처리 (트랜잭션)
```sql
-- 1. Route 상태 업데이트
UPDATE route
SET status = 'COMPLETED',
    actual_end_at = NOW()
WHERE route_id = ?;

-- 2. FlightLog 생성
INSERT INTO flight_log
  (route_id, drone_id, start_time, end_time,
   distance, battery_used, result)
VALUES (?, ?, ?, NOW(), ?, ?, 'SUCCESS');
```

## 🎬 실행 흐름

```
1. 고객: 주문 생성 (status: CREATED)
   └─> orders 테이블에 저장

2. 점주: 주문 확인 및 배송 시작
   ├─> 사용 가능한 IDLE 드론 조회
   ├─> TSP 알고리즘으로 경로 최적화
   ├─> route, route_stop 생성
   └─> 드론 시뮬레이터 시작 (status: LAUNCHED)

3. 드론 시뮬레이터:
   ├─> 2초마다 위치 계산 및 DB 저장
   ├─> WebSocket으로 실시간 브로드캐스트
   ├─> 각 경유지 도착 시 주문 완료 처리
   └─> 복귀 후 드론 상태 IDLE로 변경

4. 배송 완료:
   ├─> route.status = 'COMPLETED'
   ├─> flight_log 생성
   └─> 드론 재사용 가능
```

## 🚀 시작하기

### 1. 환경 설정

**필수 요구사항:**
- Java 17 이상
- MySQL 8.x
- Gradle 8.x

### 2. 데이터베이스 설정

```sql
CREATE DATABASE drone_delivery
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

`application.yml` 설정:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/drone_delivery
    username: root
    password: your_password
```

### 3. 애플리케이션 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

서버 실행 후 초기 데이터가 자동으로 로드됩니다 (`data.sql`).

### 4. API 문서 확인

```
http://localhost:8080/swagger-ui.html
```

## 📈 성능 최적화

### 인덱스 설계

| 테이블 | 인덱스 | 목적 |
|--------|--------|------|
| `orders` | `(store_id, status, created_at)` | 배송 대기 주문 조회 |
| `drone` | `(store_id, status)` | 사용 가능 드론 검색 |
| `route_stop` | `(route_id, stop_sequence)` | 경로 정류장 순회 |
| `route_position` | `(route_id, ts DESC)` | 최근 위치 조회 |

### N+1 문제 해결

**JOIN FETCH 활용**:
```java
@Query("SELECT o FROM Order o " +
       "JOIN FETCH o.store " +
       "JOIN FETCH o.user " +
       "LEFT JOIN FETCH o.orderItems oi " +
       "LEFT JOIN FETCH oi.product " +
       "WHERE o.orderId = :orderId")
Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);
```

## 🏆 프로젝트 성과

### 기술적 의의

1. **멀티 목적지 경로 최적화**: TSP 알고리즘 + 탑재 중량 고려
2. **실시간 추적 시스템**: WebSocket 기반 2초 단위 위치 업데이트
3. **데이터 무결성 보장**: 트랜잭션 관리로 배송 상태 실시간 반영
4. **확장 가능한 설계**: 충전소, 물류 허브 등 추가 경유지 확장 가능

### 한계 및 개선점

- **실제 하드웨어 미연동**: 드론 센서 데이터 실시간 수집 필요
- **알고리즘 단순화**: 대규모 배송지는 유전 알고리즘 등 고급 기법 필요
- **기상 정보 미반영**: 풍속, 강수 등 외부 환경 조건 고려 필요

## 🔮 향후 확장 방향

1. **실제 드론 연동**: DJI SDK, PX4 등 비행 컨트롤러 통합
2. **고급 최적화**: 유전 알고리즘, Ant Colony 등 대규모 경로 최적화
3. **동적 재조정**: 기상 정보, 교통 상황 반영한 실시간 경로 변경
4. **충전 스테이션**: 배터리 부족 시 자동 충전소 경유
5. **수요 예측 시스템**: 과거 데이터 기반 배송 수요 예측 및 사전 배치

## 📚 참고 자료

- [WebSocket 추적 가이드](docs/WEBSOCKET_TRACKING.md)
- [API 문서](http://localhost:8080/swagger-ui.html)
- [ER 다이어그램](docs/erd-diagram.png)

## 👥 팀 구성

- **6조 - 드론 멀티배송 시스템 개발팀**