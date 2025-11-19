# 드론 배송 관리 시스템 (Drone Delivery Management System)

사용자 위치 기반 드론 배송 서비스의 백엔드 API 서버입니다.

## 📋 프로젝트 개요

이 프로젝트는 데모용 드론 배송 관리 시스템으로, 다음과 같은 기능을 제공합니다:

- 사용자 위치 기반 주변 매장 조회
- 매장별 카테고리 및 상품 조회
- 장바구니 기반 주문 생성
- 10분 단위 자동 배송 배치 처리
- TSP 알고리즘을 이용한 경로 최적화
- 드론 위치 실시간 시뮬레이션 및 추적
- WebSocket을 통한 실시간 위치 정보 제공

## 🛠 기술 스택

- **Java**: 17
- **Spring Boot**: 3.5.7
- **Spring Data JPA**: ORM 및 데이터베이스 접근
- **MySQL**: 8.x
- **WebSocket (STOMP)**: 실시간 통신
- **Swagger (SpringDoc OpenAPI)**: API 문서화
- **Lombok**: 보일러플레이트 코드 감소
- **Gradle**: 빌드 도구

## 📁 프로젝트 구조 (도메인 중심 설계)

```
src/main/java/backend/databaseproject/
├── DroneDeliveryApplication.java        # 메인 애플리케이션
├── global/                              # 전역 설정
│   ├── common/                          # 공통 클래스
│   │   ├── BaseResponse.java           # API 응답 표준 포맷
│   │   ├── BaseException.java          # 커스텀 예외
│   │   └── ErrorCode.java              # 에러 코드 정의
│   ├── config/                          # 설정 클래스
│   │   ├── SwaggerConfig.java
│   │   ├── CorsConfig.java
│   │   ├── WebSocketConfig.java
│   │   └── SchedulerConfig.java
│   ├── handler/
│   │   └── GlobalExceptionHandler.java # 전역 예외 처리
│   └── util/
│       └── GeoUtils.java                # 지리 정보 유틸리티
└── domain/                              # 도메인별 구조
    ├── store/                           # 매장 도메인
    │   ├── controller/
    │   ├── service/
    │   ├── repository/
    │   ├── entity/
    │   └── dto/
    ├── product/                         # 상품 도메인
    ├── customer/                        # 고객 도메인
    ├── order/                           # 주문 도메인
    ├── drone/                           # 드론 도메인
    └── route/                           # 배송 경로 도메인
        ├── controller/
        ├── service/
        │   ├── RouteService.java
        │   ├── RouteOptimizerService.java    # TSP 경로 최적화
        │   ├── DroneSimulatorService.java    # 드론 시뮬레이터
        │   └── DeliveryBatchService.java     # 배치 처리
        ├── scheduler/
        │   └── DeliveryScheduler.java        # 10분 단위 스케줄러
        ├── repository/
        ├── entity/
        └── dto/
```

## 🚀 시작하기

### 1. 데이터베이스 설정

MySQL 데이터베이스를 생성하고 DDL을 실행하세요:

```sql
CREATE DATABASE drone_delivery CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE drone_delivery;

-- 제공된 DDL 스크립트 실행
-- (store, product, store_product, customer, drone, delivery_request, ...)
```

### 2. 환경 설정

`src/main/resources/application.yml` 파일에서 데이터베이스 연결 정보를 수정하세요:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/drone_delivery?useSSL=false&serverTimezone=Asia/Seoul
    username: root
    password: your_password_here  # 본인의 MySQL 비밀번호로 변경
```

### 3. 프로젝트 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

또는 IDE에서 `DatabaseProjectApplication.java`를 직접 실행하세요.

### 4. API 문서 확인

서버 실행 후 다음 주소에서 Swagger UI를 통해 API 문서를 확인할 수 있습니다:

```
http://localhost:8080/swagger-ui.html
```

## 📡 주요 API 엔드포인트

### 매장 관련 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/stores?lat={lat}&lng={lng}&radius={km}` | 주변 매장 조회 |
| GET | `/api/stores/{storeId}/categories` | 매장 카테고리 목록 |
| GET | `/api/stores/{storeId}/products?category={category}` | 매장 상품 목록 |

### 주문 관련 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/orders` | 주문 생성 |
| GET | `/api/orders/{orderId}` | 주문 조회 |

### 배송 경로 관련 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/routes/{routeId}` | 배송 경로 상세 조회 |
| GET | `/api/routes/{routeId}/current-position` | 드론 현재 위치 조회 |
| GET | `/api/routes/active` | 진행 중인 배송 목록 |

## 🔄 WebSocket (실시간 드론 위치)

### 연결 방법

```javascript
// SockJS + STOMP 사용
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // 특정 경로의 드론 위치 구독
    stompClient.subscribe('/topic/route/' + routeId, function(message) {
        const position = JSON.parse(message.body);
        console.log('드론 위치:', position);
        // { lat: 37.123456, lng: 127.123456, ... }
    });
});
```

### 토픽 구조

- `/topic/route/{routeId}`: 특정 배송 경로의 드론 실시간 위치

## ⚙️ 핵심 기능 설명

### 1. 10분 단위 배송 배치 처리

`DeliveryScheduler`가 매 10분마다 다음 작업을 수행합니다:

1. `CREATED` 상태의 주문들을 조회
2. 매장별로 그룹화
3. 각 그룹별로:
   - 대기 중인 드론 할당
   - 경로 최적화 (TSP 알고리즘)
   - Route 및 RouteStop 생성
   - 드론 시뮬레이터 시작

### 2. 경로 최적화 (TSP)

`RouteOptimizerService`가 Nearest Neighbor 휴리스틱을 사용하여:

- 매장 → 배송지1 → 배송지2 → ... → 매장
- 가장 짧은 경로를 계산
- Haversine 공식으로 실제 거리 계산

### 3. 드론 시뮬레이터

`DroneSimulatorService`가 비동기로 실행되며:

- 각 구간을 선형 보간하여 2초마다 위치 업데이트
- `route_position` 테이블에 저장
- WebSocket으로 실시간 브로드캐스트
- 각 정류장 도착 시 상태 업데이트

### 4. 일관된 API 응답 형식

모든 API는 `BaseResponse` 포맷을 사용합니다:

**성공 응답:**
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

**실패 응답:**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "S001",
    "message": "존재하지 않는 매장입니다."
  }
}
```

## 🧪 테스트 시나리오

### 1. 주문 생성 테스트

```bash
# 1. 주변 매장 조회
GET http://localhost:8080/api/stores?lat=37.5665&lng=126.9780&radius=5.0

# 2. 매장의 카테고리 조회
GET http://localhost:8080/api/stores/1/categories

# 3. 특정 카테고리 상품 조회
GET http://localhost:8080/api/stores/1/products?category=음료

# 4. 주문 생성
POST http://localhost:8080/api/orders
Content-Type: application/json

{
  "storeId": 1,
  "customerId": 1,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2,
      "quantity": 1
    }
  ],
  "note": "빠른 배송 부탁드립니다."
}
```

### 2. 배송 추적 테스트

```bash
# 1. 진행 중인 배송 목록 조회
GET http://localhost:8080/api/routes/active

# 2. 특정 경로 상세 조회
GET http://localhost:8080/api/routes/1

# 3. 드론 현재 위치 조회
GET http://localhost:8080/api/routes/1/current-position

# 4. WebSocket으로 실시간 위치 스트리밍
# (프론트엔드에서 구현)
```

## 🎯 데모 준비사항

### 1. 초기 데이터 삽입

데모를 위해 다음 데이터를 미리 삽입하세요:

```sql
-- 매장 데이터
INSERT INTO store (name, type, phone, address, lat, lng, delivery_radius_km, is_active) VALUES
('세븐일레븐 강남점', 'CONVENIENCE', '02-1234-5678', '서울 강남구', 37.5000, 127.0300, 3.00, 1),
('CU 홍대점', 'CONVENIENCE', '02-2345-6789', '서울 마포구', 37.5560, 126.9220, 2.50, 1);

-- 상품 데이터
INSERT INTO product (name, category, unit_weight_kg, requires_verification, is_active) VALUES
('콜라 500ml', '음료', 0.550, 0, 1),
('삼각김밥', '식품', 0.120, 0, 1),
('타이레놀', '의약품', 0.050, 1, 1);

-- 매장별 상품
INSERT INTO store_product (store_id, product_id, price, stock_qty, max_qty_per_order, is_active) VALUES
(1, 1, 1500, 100, 10, 1),
(1, 2, 1200, 50, 5, 1);

-- 고객 데이터
INSERT INTO customer (name, phone, address, lat, lng) VALUES
('홍길동', '010-1234-5678', '서울 강남구 테헤란로', 37.5050, 127.0350),
('김철수', '010-2345-6789', '서울 강남구 역삼동', 37.4980, 127.0280);

-- 드론 데이터
INSERT INTO drone (model, battery_capacity, max_payload_kg, status) VALUES
('DJI Matrice 300', 5935, 2.700, 'IDLE'),
('DJI Phantom 4', 5870, 1.500, 'IDLE');
```

### 2. 프론트엔드 연동 가이드

React 팀에게 전달할 정보:

**API Base URL:**
```
http://localhost:8080
```

**CORS 설정:**
- 허용된 Origin: `http://localhost:3000`, `http://localhost:5173`
- 허용된 메서드: GET, POST, PUT, DELETE, PATCH, OPTIONS

**WebSocket 연결:**
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);
```

**필요한 라이브러리:**
- `sockjs-client`
- `@stomp/stompjs` 또는 `stompjs`

**지도 API:**
- 네이버 지도 API 또는 Kakao 지도 API 사용
- 매장 및 드론 위치 마커 표시
- 실시간 드론 경로 폴리라인 표시

### 3. 상품 이미지 관리 방안

프론트엔드 팀과 협의된 방안:

**옵션 1: 외부 이미지 URL 사용 (추천)**
```sql
-- product 테이블에 image_url 컬럼 추가
ALTER TABLE product ADD COLUMN image_url VARCHAR(500) NULL;

-- 예시 데이터
UPDATE product SET image_url = 'https://via.placeholder.com/300x300?text=Cola' WHERE product_id = 1;
```

**옵션 2: 서버 정적 파일 제공**
```
public/images/products/
├── product_1.jpg
├── product_2.jpg
└── product_3.jpg

접속: http://localhost:8080/images/products/product_1.jpg
```

## 📊 데이터베이스 ERD

주요 테이블 관계:

```
store (매장)
  ├── store_product (매장별 상품)
  │   └── product (상품)
  └── delivery_request (배송 요청)
      ├── customer (고객)
      ├── request_item (주문 항목)
      └── route_stop_request
          └── route_stop (경로 정류장)
              └── route (배송 경로)
                  ├── drone (드론)
                  ├── route_position (위치 기록)
                  └── flight_log (비행 로그)
```

## 🔧 트러블슈팅

### 문제: 스케줄러가 실행되지 않음

**해결:** `@EnableScheduling`이 활성화되어 있는지 확인하세요.
- `SchedulerConfig.java`에 `@EnableScheduling` 추가됨

### 문제: 드론 시뮬레이터가 작동하지 않음

**해결:** `@EnableAsync`가 활성화되어 있는지 확인하세요.
- `DatabaseProjectApplication.java`에 `@EnableAsync` 추가됨

### 문제: WebSocket 연결 실패

**해결:** CORS 설정을 확인하세요.
- `WebSocketConfig.java`에서 `setAllowedOrigins()` 확인

### 문제: 빌드 실패

**해결:** Java 17 버전을 사용하고 있는지 확인하세요.
```bash
java -version
# java version "17.x.x"
```

## 📝 라이선스

이 프로젝트는 데모 목적으로 제작되었습니다.

## 👥 개발팀

- Backend: Spring Boot (Java)
- Frontend: React (JavaScript)

## 📞 문의

프로젝트 관련 문의사항은 팀 내부 채널을 통해 연락주세요.
