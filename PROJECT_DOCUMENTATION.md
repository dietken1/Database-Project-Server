# 드론 배송 관리 시스템 - 프로젝트 상세 문서

## 📑 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [시스템 아키텍처](#2-시스템-아키텍처)
3. [기술 스택 및 선택 이유](#3-기술-스택-및-선택-이유)
4. [도메인 모델 설계](#4-도메인-모델-설계)
5. [핵심 기능 상세](#5-핵심-기능-상세)
6. [API 명세](#6-api-명세)
7. [데이터베이스 설계](#7-데이터베이스-설계)
8. [실시간 통신 구조](#8-실시간-통신-구조)
9. [배치 처리 및 스케줄링](#9-배치-처리-및-스케줄링)
10. [에러 처리 전략](#10-에러-처리-전략)
11. [성능 최적화](#11-성능-최적화)
12. [보안 고려사항](#12-보안-고려사항)
13. [테스트 전략](#13-테스트-전략)
14. [배포 가이드](#14-배포-가이드)

---

## 1. 프로젝트 개요

### 1.1 프로젝트 목적

본 프로젝트는 **사용자 위치 기반 드론 배송 서비스**의 백엔드 시스템입니다. 데모 시연을 목적으로 하며, 다음과 같은 실제 비즈니스 시나리오를 구현합니다:

- 사용자가 자신의 위치에서 주변 편의점/약국을 찾음
- 원하는 매장의 상품을 카테고리별로 탐색하고 주문
- 시스템이 10분 단위로 주문을 모아 최적의 배송 경로 계산
- 드론이 매장에서 출발하여 여러 고객에게 순차 배송 후 복귀
- 사용자가 실시간으로 드론의 위치를 추적

### 1.2 주요 특징

#### ✨ 비즈니스 로직
- **위치 기반 서비스**: Haversine 공식을 이용한 정확한 거리 계산
- **경로 최적화**: TSP(Traveling Salesman Problem) 알고리즘으로 최단 경로 탐색
- **재고 관리**: 실시간 재고 확인 및 차감
- **배치 처리**: 10분 단위로 주문을 묶어 효율적인 배송

#### 🏗️ 아키텍처
- **도메인 중심 설계**: 비즈니스 로직을 도메인별로 명확히 분리
- **레이어드 아키텍처**: Controller → Service → Repository 계층 구조
- **전역 예외 처리**: 일관된 에러 응답 및 로깅
- **표준화된 API**: BaseResponse를 통한 통일된 응답 형식

#### 🔄 실시간 기능
- **WebSocket 통신**: STOMP 프로토콜을 이용한 양방향 통신
- **드론 시뮬레이터**: 실제 비행을 시뮬레이션하여 2초마다 위치 업데이트
- **비동기 처리**: Spring @Async를 통한 논블로킹 작업

### 1.3 비기능 요구사항

| 항목 | 목표 |
|------|------|
| **응답 시간** | 평균 200ms 이하 (조회 API) |
| **동시 접속** | 100+ WebSocket 연결 지원 |
| **데이터 정확도** | 위경도 소수점 6자리 (±0.11m) |
| **배치 처리** | 10분마다 안정적 실행 |
| **API 문서** | 100% Swagger 커버리지 |

---

## 2. 시스템 아키텍처

### 2.1 전체 구조도

```
┌─────────────────────────────────────────────────────────────┐
│                        클라이언트                              │
│                    (React Frontend)                          │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  네이버 지도   │  │  주문 UI     │  │ 실시간 추적   │      │
│  │   API 연동    │  │              │  │  (WebSocket) │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ HTTP/WebSocket
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Backend                        │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │              Global Layer                          │     │
│  │  - CORS Config                                     │     │
│  │  - Swagger Config                                  │     │
│  │  - WebSocket Config                                │     │
│  │  - Global Exception Handler                        │     │
│  │  - BaseResponse / ErrorCode                        │     │
│  └────────────────────────────────────────────────────┘     │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Controller Layer                       │    │
│  │  StoreController │ OrderController │ RouteController│    │
│  └─────────────────────────────────────────────────────┘    │
│                           │                                  │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Service Layer                          │    │
│  │                                                     │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │    │
│  │  │ StoreService │  │ OrderService │  │RouteService│ │    │
│  │  └──────────────┘  └──────────────┘  └──────────┘ │    │
│  │                                                     │    │
│  │  ┌────────────────────────────────────────────┐   │    │
│  │  │        Route Domain Services               │   │    │
│  │  │  - RouteOptimizerService (TSP)             │   │    │
│  │  │  - DroneSimulatorService (@Async)          │   │    │
│  │  │  - DeliveryBatchService                    │   │    │
│  │  └────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────┘    │
│                           │                                  │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Repository Layer (JPA)                 │    │
│  │  StoreRepo │ OrderRepo │ RouteRepo │ DroneRepo ... │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Scheduler                              │    │
│  │  DeliveryScheduler (@Scheduled, cron: */10)         │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ JDBC
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                      MySQL Database                          │
│                                                              │
│  Store │ Product │ User │ Drone │ Order                 │
│  Route │ RouteStop │ RoutePosition │ FlightLog ...          │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 패키지 구조 (도메인 중심 설계)

```
backend.databaseproject/
│
├── DroneDeliveryApplication.java    # 메인 클래스 (@EnableAsync)
│
├── global/                           # 전역 공통 기능
│   ├── common/
│   │   ├── BaseResponse.java        # 표준 응답 포맷
│   │   ├── BaseException.java       # 비즈니스 예외
│   │   └── ErrorCode.java           # 에러 코드 enum
│   ├── config/
│   │   ├── SwaggerConfig.java       # OpenAPI 설정
│   │   ├── CorsConfig.java          # CORS 허용
│   │   ├── WebSocketConfig.java     # STOMP 설정
│   │   └── SchedulerConfig.java     # @EnableScheduling
│   ├── handler/
│   │   └── GlobalExceptionHandler.java  # @RestControllerAdvice
│   └── util/
│       └── GeoUtils.java             # Haversine 거리 계산
│
└── domain/                           # 도메인별 구조
    ├── store/                        # 매장 도메인
    │   ├── controller/
    │   │   └── StoreController.java
    │   ├── service/
    │   │   └── StoreService.java
    │   ├── repository/
    │   │   ├── StoreRepository.java
    │   │   └── StoreProductRepository.java
    │   ├── entity/
    │   │   ├── Store.java
    │   │   ├── StoreProduct.java
    │   │   └── StoreType.java (enum)
    │   └── dto/
    │       └── response/
    │           ├── StoreResponse.java
    │           ├── CategoryResponse.java
    │           └── ProductResponse.java
    │
    ├── product/                      # 상품 도메인
    │   ├── entity/
    │   │   └── Product.java
    │   └── repository/
    │       └── ProductRepository.java
    │
    ├── user/                         # 사용자 도메인
    │   ├── controller/
    │   │   └── UserController.java
    │   ├── service/
    │   │   └── UserService.java
    │   ├── entity/
    │   │   ├── User.java
    │   │   └── UserRole.java (enum)
    │   ├── repository/
    │   │   └── UserRepository.java
    │   └── dto/
    │       └── response/
    │           └── UserResponse.java
    │
    ├── order/                        # 주문 도메인
    │   ├── controller/
    │   │   └── OrderController.java
    │   ├── service/
    │   │   └── OrderService.java
    │   ├── repository/
    │   │   ├── OrderRepository.java
    │   │   └── OrderItemRepository.java
    │   ├── entity/
    │   │   ├── Order.java
    │   │   ├── OrderItem.java
    │   │   └── OrderStatus.java (enum)
    │   └── dto/
    │       ├── request/
    │       │   ├── OrderCreateRequest.java
    │       │   └── OrderItemRequest.java
    │       └── response/
    │           ├── OrderResponse.java
    │           └── OrderItemResponse.java
    │
    ├── drone/                        # 드론 도메인
    │   ├── entity/
    │   │   ├── Drone.java
    │   │   └── DroneStatus.java (enum)
    │   └── repository/
    │       └── DroneRepository.java
    │
    └── route/                        # 배송 경로 도메인 (핵심)
        ├── controller/
        │   └── RouteController.java
        ├── service/
        │   ├── RouteService.java
        │   ├── RouteOptimizerService.java     # TSP 알고리즘
        │   ├── DroneSimulatorService.java     # @Async 시뮬레이션
        │   └── DeliveryBatchService.java      # 배치 처리
        ├── scheduler/
        │   └── DeliveryScheduler.java         # @Scheduled
        ├── repository/
        │   ├── RouteRepository.java
        │   ├── RouteStopRepository.java
        │   ├── RoutePositionRepository.java
        │   └── FlightLogRepository.java
        ├── entity/
        │   ├── Route.java
        │   ├── RouteStop.java
        │   ├── RoutePosition.java
        │   ├── FlightLog.java
        │   ├── RouteStatus.java (enum)
        │   ├── StopType.java (enum)
        │   ├── StopStatus.java (enum)
        │   └── FlightResult.java (enum)
        └── dto/
            └── response/
                ├── RouteResponse.java
                ├── RouteStopResponse.java
                └── DronePositionResponse.java
```

### 2.3 계층별 책임

| 계층 | 책임 | 예시 |
|------|------|------|
| **Controller** | HTTP 요청/응답 처리, Validation | `@RestController`, `@GetMapping`, `@Valid` |
| **Service** | 비즈니스 로직, 트랜잭션 관리 | 재고 확인, 거리 계산, 경로 최적화 |
| **Repository** | 데이터 접근, 쿼리 실행 | JPA, `@Query`, Custom Query |
| **Entity** | 도메인 모델, 비즈니스 규칙 | 상태 변경 메서드, 양방향 연관관계 |
| **DTO** | 데이터 전송, 응답 변환 | Request/Response 객체 |

---

## 3. 기술 스택 및 선택 이유

### 3.1 백엔드 프레임워크

#### Spring Boot 3.5.7
- **선택 이유**:
  - 엔터프라이즈급 안정성 및 풍부한 생태계
  - Auto Configuration으로 빠른 개발
  - Spring Data JPA로 ORM 간편 처리
  - Spring WebSocket으로 실시간 통신 지원
  - Production-ready 기능 (Actuator, Logging 등)

### 3.2 데이터베이스

#### MySQL 8.x
- **선택 이유**:
  - 공간 데이터 함수 지원 (Haversine 거리 계산)
  - 트랜잭션 ACID 보장
  - 대용량 데이터 처리 성능
  - 널리 사용되어 레퍼런스 풍부
  - 무료 오픈소스

### 3.3 ORM

#### Spring Data JPA (Hibernate)
- **선택 이유**:
  - 객체-관계 매핑으로 생산성 향상
  - JPQL로 복잡한 쿼리 처리
  - Lazy Loading으로 N+1 문제 방지
  - 네이티브 쿼리 지원 (거리 계산 등)

### 3.4 실시간 통신

#### Spring WebSocket (STOMP over SockJS)
- **선택 이유**:
  - 양방향 실시간 통신 (드론 위치 스트리밍)
  - SockJS로 브라우저 호환성 보장
  - STOMP 프로토콜로 메시지 라우팅 간편
  - Spring 생태계와 자연스러운 통합

### 3.5 API 문서화

#### SpringDoc OpenAPI 3.0 (Swagger UI)
- **선택 이유**:
  - 코드 기반 자동 문서 생성
  - 인터랙티브 테스트 UI 제공
  - Spring Boot 3.x 공식 지원
  - 프론트엔드 팀과의 협업 도구

### 3.6 빌드 도구

#### Gradle 8.x
- **선택 이유**:
  - Maven보다 빠른 빌드 속도
  - Groovy/Kotlin DSL로 유연한 설정
  - Incremental Build 지원
  - Spring Initializr 기본 제공

### 3.7 유틸리티

#### Lombok
- **선택 이유**:
  - Boilerplate 코드 감소 (Getter, Constructor 등)
  - `@Slf4j`로 로깅 간편화
  - `@Builder`로 객체 생성 패턴 지원

---

## 4. 도메인 모델 설계

### 4.1 도메인 개요

시스템은 7개의 핵심 도메인으로 구성됩니다:

```
┌─────────┐      ┌─────────┐      ┌─────────┐
│  Store  │◄────►│ Product │      │  User   │
│  (매장)  │      │  (상품)  │      │(사용자)  │
└────┬────┘      └────┬────┘      └────┬────┘
     │                │                │
     │ owner_id       │                │ role:
     │ (OWNER)        │                │ CUSTOMER
     │                ▼                │ OWNER
     │         ┌──────────────────────────┐
     │         │    StoreProduct          │
     │         │   (매장별 판매 상품)        │
     │         └──────────────────────────┘
     │                │                │
     │ 1:N (drones)   │                │
     ▼                ▼                ▼
┌──────────┐  ┌──────────────────────────┐      ┌──────────┐
│  Drone   │  │        Order             │◄────►│   User   │
│ (드론)    │  │      (주문)               │      │(CUSTOMER)│
└────┬─────┘  └──────┬───────────────────┘      └──────────┘
     │               │
     │ N:1 (store)   │ 1:N
     │               ▼
     │        ┌──────────────────────────┐
     │        │      OrderItem           │
     │        │    (주문 항목)            │
     │        └──────────────────────────┘
     │               │
     │ 1:N           │
     ▼               ▼
┌─────────┐   ┌──────────────────────────┐
│  Route  │   │  RouteStopOrder          │
│(배송경로)│   │  (경로-주문 매핑)          │
└────┬────┘   └──────┬───────────────────┘
     │               │
     │ 1:N           ▼
     ▼        ┌──────────────────────────┐
┌──────────┐  │     RouteStop            │
│RoutePos. │  │     (정류장)              │
└──────────┘  └──────────────────────────┘
┌──────────┐
│FlightLog │
└──────────┘

주요 관계:
- Store 1:N Drone (각 매장은 여러 드론 보유)
- Drone N:1 Store (각 드론은 특정 매장 소속)
- Store 1:N User(OWNER) (각 매장은 1명의 점주 소유)
- Drone 1:N Route (드론은 여러 배송 경로 수행)
```

### 4.2 주요 Entity 설명

#### Store (매장)
```java
@Entity
public class Store {
    @Id @GeneratedValue
    private Long storeId;

    private String name;                    // 매장명
    @Enumerated(EnumType.STRING)
    private StoreType type;                 // CONVENIENCE, PHARMACY, OTHER
    private BigDecimal lat;                 // 위도 (소수점 6자리)
    private BigDecimal lng;                 // 경도 (소수점 6자리)
    private BigDecimal deliveryRadiusKm;    // 배송 가능 반경
    private Boolean isActive;               // 운영 여부
}
```
**역할**: 드론 배송 거점 (출발/복귀 지점)

#### Product (상품)
```java
@Entity
public class Product {
    @Id @GeneratedValue
    private Long productId;

    private String name;                    // 상품명
    private String category;                // 카테고리 (음료, 식품, 의약품 등)
    private BigDecimal unitWeightKg;        // 단위 무게
    private Boolean requiresVerification;   // 연령 확인 필요 여부
}
```
**역할**: 판매 가능한 상품 마스터

#### StoreProduct (매장별 판매 상품)
```java
@Entity
@IdClass(StoreProductId.class)
public class StoreProduct {
    @Id @ManyToOne
    private Store store;

    @Id @ManyToOne
    private Product product;

    private Integer price;                  // 판매가
    private Integer stockQty;               // 재고 수량
    private Integer maxQtyPerOrder;         // 최대 주문 수량

    // 재고 감소 메서드
    public void decreaseStock(int quantity) {
        this.stockQty -= quantity;
    }
}
```
**역할**: 매장마다 다른 가격/재고 관리

#### User (사용자)
```java
@Entity
@Table(name = "user")
public class User {
    @Id @GeneratedValue
    private Long userId;

    private String name;                    // 사용자명
    private String phone;                   // 전화번호
    private String address;                 // 주소
    private BigDecimal lat;                 // 위도
    private BigDecimal lng;                 // 경도

    @Enumerated(EnumType.STRING)
    private UserRole role;                  // CUSTOMER, OWNER

    private LocalDateTime registeredAt;     // 등록일시
}
```
**역할**: 고객(CUSTOMER) 또는 매장 점주(OWNER)

#### Order (주문)
```java
@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue
    private Long orderId;

    @ManyToOne
    private Store store;                    // 출발 매장

    @ManyToOne
    private User user;                      // 주문 사용자

    private BigDecimal originLat;           // 출발지 위도
    private BigDecimal originLng;           // 출발지 경도
    private BigDecimal destLat;             // 목적지 위도
    private BigDecimal destLng;             // 목적지 경도

    private BigDecimal totalWeightKg;       // 총 무게
    private Integer totalAmount;            // 총 금액
    private Integer itemCount;              // 항목 수

    @Enumerated(EnumType.STRING)
    private OrderStatus status;             // CREATED, ASSIGNED, FULFILLED, CANCELED

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;     // 주문 항목들

    // 비즈니스 메서드
    public void assignDelivery() {
        this.status = OrderStatus.ASSIGNED;
        this.assignedAt = LocalDateTime.now();
    }
}
```
**역할**: 사용자의 배송 요청 (주문서)

#### Drone (드론)
```java
@Entity
public class Drone {
    @Id @GeneratedValue
    private Long droneId;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;                    // 소속 매장

    private String model;                   // 모델명
    private Integer batteryCapacity;        // 배터리 용량 (mAh)
    private BigDecimal maxPayloadKg;        // 최대 적재량 (kg)

    @Enumerated(EnumType.STRING)
    private DroneStatus status;             // IDLE, IN_FLIGHT, CHARGING, MAINTENANCE
}
```
**역할**: 배송을 수행하는 드론 (매장 소속)
**관계**:
- N:1 Store (각 드론은 특정 매장 소속)
- 1:N Route (드론은 여러 배송 경로 수행)

#### Route (배송 경로)
```java
@Entity
public class Route {
    @Id @GeneratedValue
    private Long routeId;

    @ManyToOne
    private Drone drone;                    // 할당된 드론

    @ManyToOne
    private Store store;                    // 출발/복귀 매장

    @Enumerated(EnumType.STRING)
    private RouteStatus status;             // PLANNED, LAUNCHED, IN_PROGRESS, COMPLETED

    private LocalDateTime plannedStartAt;
    private LocalDateTime actualStartAt;
    private BigDecimal plannedTotalDistanceKm;  // 계획된 총 거리
    private String heuristic;               // 사용한 알고리즘 (예: "2-opt")

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    private List<RouteStop> routeStops;     // 경유지들 (PICKUP → DROP들 → RETURN)

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    private List<RoutePosition> routePositions;  // 위치 기록들
}
```
**역할**: 한 번의 드론 출격 임무

#### RouteStop (경로 정류장)
```java
@Entity
public class RouteStop {
    @Id @GeneratedValue
    private Long stopId;

    @ManyToOne
    private Route route;

    private Integer stopSeq;                // 순서 (1, 2, 3, ...)

    @Enumerated(EnumType.STRING)
    private StopType type;                  // PICKUP, DROP, RETURN

    @Enumerated(EnumType.STRING)
    private StopStatus status;              // PENDING, ARRIVED, DEPARTED

    private BigDecimal lat;
    private BigDecimal lng;

    @ManyToOne
    private Store store;                    // PICKUP/RETURN인 경우

    @ManyToOne
    private User user;                      // DROP인 경우
}
```
**역할**: 경로상의 각 정류장 (픽업 → 배송1 → 배송2 → ... → 복귀)

#### RoutePosition (드론 위치)
```java
@Entity
public class RoutePosition {
    @Id @GeneratedValue
    private Long posId;

    @ManyToOne
    private Route route;

    private BigDecimal lat;                 // 현재 위도
    private BigDecimal lng;                 // 현재 경도
    private BigDecimal speedMps;            // 속도 (m/s)
    private BigDecimal batteryPct;          // 배터리 잔량 (%)

    private LocalDateTime ts;               // 측정 시각
}
```
**역할**: 드론의 실시간 위치 저장 (2초마다 INSERT)

---

## 5. 핵심 기능 상세

### 5.1 주변 매장 조회

#### 흐름도
```
사용자 위치 (위도, 경도)
      ↓
[StoreController]
  GET /api/stores?lat=37.5665&lng=126.9780&radius=5.0
      ↓
[StoreService.getStoresNearby()]
      ↓
[StoreRepository.findStoresWithinRadius()]
  ✓ Native Query로 Haversine 거리 계산
  ✓ 반경 내 매장만 필터링
  ✓ 거리 순 정렬
      ↓
[GeoUtils.calculateDistance()]
  각 매장까지의 정확한 거리 계산
      ↓
List<StoreResponse> 반환
  (매장 정보 + 사용자로부터의 거리)
```

#### 핵심 코드

**Repository (Native Query)**
```java
@Query(value = """
    SELECT s.* FROM store s
    WHERE s.is_active = 1
    AND (6371 * acos(
        cos(radians(:lat)) * cos(radians(s.lat)) *
        cos(radians(s.lng) - radians(:lng)) +
        sin(radians(:lat)) * sin(radians(s.lat))
    )) <= :radiusKm
    ORDER BY (...)
    """, nativeQuery = true)
List<Store> findStoresWithinRadius(
    @Param("lat") BigDecimal lat,
    @Param("lng") BigDecimal lng,
    @Param("radiusKm") BigDecimal radiusKm
);
```

**Haversine 공식 (GeoUtils)**
```java
public static double calculateDistance(
    double lat1, double lng1,
    double lat2, double lng2
) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
             + Math.cos(Math.toRadians(lat1))
             * Math.cos(Math.toRadians(lat2))
             * Math.sin(dLng / 2) * Math.sin(dLng / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return 6371.0 * c;  // km
}
```

**정확도**: 소수점 6자리 (±0.11m 오차)

---

### 5.2 주문 생성 및 재고 관리

#### 흐름도
```
사용자 장바구니
      ↓
[OrderController]
  POST /api/orders
  RequestBody: {
    storeId: 1,
    userId: 1,
    items: [{productId: 1, quantity: 2}, ...]
  }
      ↓
[OrderService.createOrder()]

  1️⃣ 매장 존재 및 활성화 확인
     └─ 없으면 STORE_NOT_FOUND 예외

  2️⃣ 사용자 존재 확인
     └─ 없으면 USER_NOT_FOUND 예외

  3️⃣ 주문 항목 검증
     └─ 비어있으면 ORDER_ITEMS_EMPTY 예외

  4️⃣ 각 상품별 검증 (Loop)
     ├─ StoreProduct 조회 (해당 매장에서 판매하는가?)
     ├─ 재고 확인 (stockQty >= quantity?)
     │  └─ 부족하면 PRODUCT_OUT_OF_STOCK 예외
     ├─ 최대 수량 확인 (quantity <= maxQtyPerOrder?)
     │  └─ 초과하면 PRODUCT_EXCEED_MAX_QUANTITY 예외
     └─ 총 무게/금액 계산

  5️⃣ Order 생성
     ├─ originLat/Lng ← Store 위치
     ├─ destLat/Lng ← User 위치
     ├─ totalWeightKg, totalAmount, itemCount
     └─ status = CREATED

  6️⃣ OrderItem 생성 및 추가

  7️⃣ ⭐ 재고 차감
     └─ storeProduct.decreaseStock(quantity)

  8️⃣ 트랜잭션 커밋
      ↓
OrderCreateResponse 반환
```

#### 트랜잭션 관리

```java
@Service
@Transactional  // ← 전체 메서드가 하나의 트랜잭션
public class OrderService {

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        // ... 검증 로직 ...

        // 재고 차감 (영속성 컨텍스트에서 관리)
        for (OrderItemRequest item : request.getItems()) {
            StoreProduct sp = storeProductRepository.findById(...);
            sp.decreaseStock(item.getQuantity());  // ← UPDATE 쿼리 자동 생성
        }

        // Order 저장
        Order saved = orderRepository.save(order);

        return OrderResponse.from(saved);
        // ← 여기서 트랜잭션 커밋 (재고 감소 확정)
    }
}
```

**ACID 보장**:
- **Atomicity**: 주문 생성과 재고 차감이 모두 성공하거나 모두 실패
- **Consistency**: 재고는 항상 0 이상 (CHECK 제약조건)
- **Isolation**: 동시에 같은 상품 주문 시 격리 수준에 따라 처리
- **Durability**: 커밋 후 데이터베이스에 영구 저장

---

### 5.3 경로 최적화 (TSP 알고리즘)

#### 문제 정의

**입력**:
- 출발지: 매장 (lat, lng)
- 배송지들: N개의 고객 위치
- 복귀지: 동일한 매장

**목표**: 총 이동 거리를 최소화하는 방문 순서 찾기

#### 알고리즘: Nearest Neighbor (NN) 휴리스틱

```java
@Service
public class RouteOptimizerService {

    public List<Order> optimizeRoute(
        List<Order> orders,
        Store store
    ) {
        List<Order> optimized = new ArrayList<>();
        List<Order> unvisited = new ArrayList<>(orders);

        // 현재 위치 = 매장
        double currentLat = store.getLat().doubleValue();
        double currentLng = store.getLng().doubleValue();

        // 모든 배송지 방문할 때까지 반복
        while (!unvisited.isEmpty()) {
            Order nearest = null;
            double minDistance = Double.MAX_VALUE;

            // 현재 위치에서 가장 가까운 배송지 찾기
            for (Order order : unvisited) {
                double distance = GeoUtils.calculateDistance(
                    currentLat, currentLng,
                    order.getDestLat().doubleValue(),
                    order.getDestLng().doubleValue()
                );

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = order;
                }
            }

            // 가장 가까운 곳을 다음 방문지로 선택
            optimized.add(nearest);
            unvisited.remove(nearest);

            // 현재 위치 업데이트
            currentLat = nearest.getDestLat().doubleValue();
            currentLng = nearest.getDestLng().doubleValue();
        }

        return optimized;  // 최적화된 순서
    }
}
```

#### 시간 복잡도

- **Nearest Neighbor**: O(N²)
- N = 배송지 개수 (일반적으로 10개 이하)
- 실시간 처리 가능한 수준

#### 개선 가능한 알고리즘

| 알고리즘 | 시간 복잡도 | 정확도 | 비고 |
|---------|-----------|--------|------|
| Nearest Neighbor | O(N²) | 근사해 | 현재 구현 |
| 2-opt | O(N²) | 더 나은 근사해 | 추후 고도화 |
| Genetic Algorithm | O(N × G) | 매우 좋음 | G = 세대 수 |
| Exact (DP) | O(2ᴺ × N²) | 최적해 | N≤20 정도만 가능 |

---

### 5.4 드론 시뮬레이터

#### 목적
실제 드론 없이 데모를 위해 드론 비행을 시뮬레이션하고, 실시간으로 위치를 추적 가능하게 함

#### 비동기 실행 구조

```java
@Service
@EnableAsync  // ← Main Application에 설정
public class DroneSimulatorService {

    private final SimpMessagingTemplate messagingTemplate;  // WebSocket

    @Async  // ← 별도 스레드에서 실행
    public void simulateFlight(Long routeId) {
        Route route = routeRepository.findByIdWithDetails(routeId)
            .orElseThrow(() -> new BaseException(ErrorCode.ROUTE_NOT_FOUND));

        // 1. 경로 상태 변경
        route.changeStatus(RouteStatus.LAUNCHED);
        route.setActualStartAt(LocalDateTime.now());

        List<RouteStop> stops = route.getRouteStops();

        // 2. 각 구간 순회
        for (int i = 0; i < stops.size() - 1; i++) {
            RouteStop from = stops.get(i);
            RouteStop to = stops.get(i + 1);

            // 3. 구간 시뮬레이션 (2초마다 위치 업데이트)
            simulateSegment(route, from, to);
        }

        // 4. 경로 완료
        route.changeStatus(RouteStatus.COMPLETED);
        route.setActualEndAt(LocalDateTime.now());

        // 5. FlightLog 생성
        createFlightLog(route);
    }

    private void simulateSegment(Route route, RouteStop from, RouteStop to) {
        double startLat = from.getLat().doubleValue();
        double startLng = from.getLng().doubleValue();
        double endLat = to.getLat().doubleValue();
        double endLng = to.getLng().doubleValue();

        // 거리 계산
        double distance = GeoUtils.calculateDistance(
            startLat, startLng, endLat, endLng
        );

        // 소요 시간 계산 (평균 속도 30km/h = 8.33m/s)
        double durationSeconds = (distance * 1000) / 8.33;
        int steps = (int) (durationSeconds / 2);  // 2초마다 업데이트

        // 선형 보간으로 위치 업데이트
        for (int step = 0; step <= steps; step++) {
            double fraction = (double) step / steps;  // 0.0 ~ 1.0

            double[] position = GeoUtils.interpolate(
                startLat, startLng,
                endLat, endLng,
                fraction
            );

            // RoutePosition 저장
            RoutePosition rp = RoutePosition.builder()
                .route(route)
                .lat(new BigDecimal(position[0]))
                .lng(new BigDecimal(position[1]))
                .speedMps(new BigDecimal(8.33))
                .batteryPct(new BigDecimal(100 - (step * 0.5)))
                .build();
            routePositionRepository.save(rp);

            // WebSocket으로 브로드캐스트
            DronePositionResponse response = DronePositionResponse.from(rp);
            messagingTemplate.convertAndSend(
                "/topic/route/" + route.getRouteId(),
                response
            );

            // 2초 대기
            Thread.sleep(2000);
        }

        // 목적지 도착
        to.arrive();  // status = ARRIVED
    }
}
```

#### 선형 보간 (Linear Interpolation)

```java
public static double[] interpolate(
    double lat1, double lng1,
    double lat2, double lng2,
    double fraction  // 0.0 ~ 1.0
) {
    double lat = lat1 + (lat2 - lat1) * fraction;
    double lng = lng1 + (lng2 - lng1) * fraction;
    return new double[]{lat, lng};
}
```

**예시**:
- 시작: (37.5000, 127.0300)
- 종료: (37.5050, 127.0350)
- fraction = 0.5 (50% 지점)
- 결과: (37.5025, 127.0325)

---

### 5.5 배치 처리 및 스케줄링

#### 스케줄러 설정

```java
@Component
@Slf4j
public class DeliveryScheduler {

    @Scheduled(cron = "0 */10 * * * *")  // 매 10분마다
    public void scheduleBatch() {
        log.info("배송 배치 처리 스케줄러 시작");
        deliveryBatchService.processBatch();
        log.info("배송 배치 처리 스케줄러 완료");
    }
}
```

**Cron 표현식**: `"0 */10 * * * *"`
- 초: 0
- 분: */10 (매 10분마다 - 0, 10, 20, 30, 40, 50)
- 시: * (모든 시)
- 일: * (모든 날)
- 월: * (모든 월)
- 요일: * (모든 요일)

#### 배치 처리 로직

```java
@Service
@Transactional
public class DeliveryBatchService {

    public void processBatch() {
        // 1. CREATED 상태의 주문들 조회
        List<Order> pendingOrders =
            orderRepository.findByStatus(OrderStatus.CREATED);

        if (pendingOrders.isEmpty()) {
            log.info("처리할 주문이 없습니다.");
            return;
        }

        // 2. 매장별로 그룹화
        Map<Long, List<Order>> groupedByStore = pendingOrders.stream()
            .collect(Collectors.groupingBy(order -> order.getStore().getStoreId()));

        // 3. 각 매장별로 처리
        for (Map.Entry<Long, List<Order>> entry : groupedByStore.entrySet()) {
            Long storeId = entry.getKey();
            List<Order> orders = entry.getValue();

            try {
                processStoreDeliveries(storeId, orders);
            } catch (Exception e) {
                log.error("매장 {} 배송 처리 실패", storeId, e);
            }
        }
    }

    private void processStoreDeliveries(Long storeId, List<Order> orders) {
        // 1. 매장 조회
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));

        // 2. 해당 매장의 대기 중인 드론 조회
        Drone drone = droneRepository.findFirstByStoreAndStatus(store, DroneStatus.IDLE)
            .orElseThrow(() -> new BaseException(ErrorCode.DRONE_NOT_AVAILABLE));

        // 3. 경로 최적화
        List<Order> optimizedOrders =
            routeOptimizerService.optimizeRoute(orders, store);

        // 4. Route 생성
        Route route = Route.builder()
            .drone(drone)
            .store(store)
            .status(RouteStatus.PLANNED)
            .heuristic("Nearest Neighbor")
            .build();
        routeRepository.save(route);

        // 5. RouteStop 생성
        int seq = 1;

        // 5-1. PICKUP (매장에서 물건 싣기)
        RouteStop pickup = RouteStop.builder()
            .route(route)
            .stopSeq(seq++)
            .type(StopType.PICKUP)
            .name(store.getName())
            .lat(store.getLat())
            .lng(store.getLng())
            .store(store)
            .build();
        routeStopRepository.save(pickup);

        // 5-2. DROP들 (각 고객에게 배송)
        for (Order order : optimizedOrders) {
            RouteStop drop = RouteStop.builder()
                .route(route)
                .stopSeq(seq++)
                .type(StopType.DROP)
                .name(order.getUser().getName())
                .lat(order.getDestLat())
                .lng(order.getDestLng())
                .user(order.getUser())
                .build();
            routeStopRepository.save(drop);

            // 주문 상태 변경
            order.assignDelivery();  // CREATED → ASSIGNED
        }

        // 5-3. RETURN (매장으로 복귀)
        RouteStop returnStop = RouteStop.builder()
            .route(route)
            .stopSeq(seq)
            .type(StopType.RETURN)
            .name(store.getName())
            .lat(store.getLat())
            .lng(store.getLng())
            .store(store)
            .build();
        routeStopRepository.save(returnStop);

        // 6. 드론 상태 변경
        drone.changeStatus(DroneStatus.IN_FLIGHT);

        // 7. 드론 시뮬레이터 시작 (비동기)
        droneSimulatorService.simulateFlight(route.getRouteId());

        log.info("매장 {} 배송 경로 생성 완료: {} 건", storeId, requests.size());
    }
}
```

#### 실행 예시

```
[10:00:00] 배송 배치 처리 스케줄러 시작
[10:00:01] CREATED 상태 주문 5건 조회
[10:00:01] 매장별 그룹화: {1: [주문1, 주문2, 주문3], 2: [주문4, 주문5]}
[10:00:02] 매장 1 처리 시작
[10:00:02] 대기 중인 드론 할당: Drone#1
[10:00:03] 경로 최적화 완료: 주문1 → 주문3 → 주문2
[10:00:04] Route#1 생성 (5개 Stop: PICKUP → DROP × 3 → RETURN)
[10:00:05] 드론 시뮬레이터 시작 (비동기)
[10:00:05] 매장 1 배송 경로 생성 완료: 3건
[10:00:06] 매장 2 처리 시작
...
[10:00:15] 배송 배치 처리 스케줄러 완료
```

---

## 6. API 명세

### 6.1 공통 응답 형식

모든 API는 `BaseResponse<T>` 형식을 따릅니다.

#### 성공 응답
```json
{
  "success": true,
  "data": {
    // 실제 데이터
  },
  "error": null
}
```

#### 실패 응답
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

### 6.2 매장 API

#### 1. 주변 매장 조회

**요청**
```http
GET /api/stores?lat=37.5665&lng=126.9780&radius=5.0
```

**파라미터**
| 이름 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| lat | BigDecimal | O | 사용자 위도 | 37.5665 |
| lng | BigDecimal | O | 사용자 경도 | 126.9780 |
| radius | BigDecimal | X | 검색 반경 (km) | 5.0 (기본값) |

**응답**
```json
{
  "success": true,
  "data": [
    {
      "storeId": 1,
      "name": "세븐일레븐 강남점",
      "type": "CONVENIENCE",
      "phone": "02-1234-5678",
      "address": "서울 강남구",
      "lat": 37.5000,
      "lng": 127.0300,
      "deliveryRadiusKm": 3.00,
      "distanceKm": 1.25
    }
  ],
  "error": null
}
```

#### 2. 매장 카테고리 목록

**요청**
```http
GET /api/stores/1/categories
```

**응답**
```json
{
  "success": true,
  "data": [
    {"category": "음료"},
    {"category": "식품"},
    {"category": "의약품"}
  ],
  "error": null
}
```

#### 3. 매장 상품 목록

**요청**
```http
GET /api/stores/1/products?category=음료
```

**파라미터**
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| category | String | X | 카테고리 (없으면 전체) |

**응답**
```json
{
  "success": true,
  "data": [
    {
      "productId": 1,
      "name": "콜라 500ml",
      "category": "음료",
      "unitWeightKg": 0.550,
      "price": 1500,
      "stockQty": 100,
      "maxQtyPerOrder": 10
    }
  ],
  "error": null
}
```

### 6.3 주문 API

#### 1. 주문 생성

**요청**
```http
POST /api/orders
Content-Type: application/json

{
  "storeId": 1,
  "userId": 1,
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

**응답 (201 Created)**
```json
{
  "orderId": 1
}
        "unitWeightKg": 0.550,
        "subtotal": 3000
      },
      {
        "requestItemId": 2,
        "productId": 2,
        "productName": "삼각김밥",
        "quantity": 1,
        "unitPrice": 1200,
        "unitWeightKg": 0.120,
        "subtotal": 1200
      }
    ],
    "note": "빠른 배송 부탁드립니다."
  },
  "error": null
}
```

#### 2. 주문 조회

**요청**
```http
GET /api/orders/1
```

**응답**
```json
{
  "success": true,
  "data": {
    "requestId": 1,
    "status": "ASSIGNED",
    "assignedAt": "2025-11-20T14:40:00",
    // ... (나머지 동일)
  },
  "error": null
}
```

### 6.4 배송 경로 API

#### 1. 배송 경로 조회

**요청**
```http
GET /api/routes/1
```

**응답**
```json
{
  "success": true,
  "data": {
    "routeId": 1,
    "droneId": 1,
    "droneModel": "DJI Matrice 300",
    "storeId": 1,
    "storeName": "세븐일레븐 강남점",
    "status": "IN_PROGRESS",
    "createdAt": "2025-11-20T14:40:00",
    "launchedAt": "2025-11-20T14:40:05",
    "totalDistanceKm": 5.23,
    "totalWeightKg": 3.450,
    "estimatedDurationMin": 15,
    "stops": [
      {
        "stopId": 1,
        "stopSeq": 1,
        "type": "PICKUP",
        "name": "세븐일레븐 강남점",
        "lat": 37.5000,
        "lng": 127.0300,
        "status": "DEPARTED",
        "arrivedAt": "2025-11-20T14:40:05",
        "departedAt": "2025-11-20T14:41:00"
      },
      {
        "stopId": 2,
        "stopSeq": 2,
        "type": "DROP",
        "name": "홍길동",
        "lat": 37.5050,
        "lng": 127.0350,
        "status": "ARRIVED",
        "arrivedAt": "2025-11-20T14:43:00"
      },
      // ... 나머지 Stop들
    ]
  },
  "error": null
}
```

#### 2. 드론 현재 위치 조회

**요청**
```http
GET /api/routes/1/current-position
```

**응답**
```json
{
  "success": true,
  "data": {
    "routeId": 1,
    "lat": 37.5025,
    "lng": 127.0325,
    "altitudeM": 50.0,
    "speedKmh": 30.0,
    "battery": 95,
    "recordedAt": "2025-11-20T14:42:30"
  },
  "error": null
}
```

#### 3. 진행 중인 배송 목록

**요청**
```http
GET /api/routes/active
```

**응답**
```json
{
  "success": true,
  "data": [
    {
      "routeId": 1,
      "status": "IN_PROGRESS",
      // ... (routeId=1 상세 정보)
    },
    {
      "routeId": 2,
      "status": "LAUNCHED",
      // ... (routeId=2 상세 정보)
    }
  ],
  "error": null
}
```

### 6.5 에러 코드

| 코드 | HTTP 상태 | 메시지 |
|------|----------|--------|
| C001 | 400 | 잘못된 입력값입니다. |
| C002 | 405 | 지원하지 않는 HTTP 메서드입니다. |
| C003 | 500 | 서버 내부 오류가 발생했습니다. |
| **S001** | 404 | 존재하지 않는 매장입니다. |
| **S002** | 400 | 운영 중이지 않은 매장입니다. |
| **P001** | 404 | 존재하지 않는 상품입니다. |
| **P003** | 400 | 재고가 부족합니다. |
| **P004** | 400 | 최대 주문 수량을 초과했습니다. |
| **CU001** | 404 | 존재하지 않는 고객입니다. |
| **O001** | 404 | 존재하지 않는 주문입니다. |
| **O004** | 400 | 주문 항목이 비어있습니다. |
| **D002** | 400 | 사용 가능한 드론이 없습니다. |
| **R001** | 404 | 존재하지 않는 배송 경로입니다. |
| **R005** | 404 | 드론 위치 정보를 찾을 수 없습니다. |

---

## 7. 데이터베이스 설계

### 7.1 ERD (Entity Relationship Diagram)

```
┌─────────────────┐
│     STORE       │
│  (매장)          │
├─────────────────┤
│ PK store_id     │
│    name         │
│    type         │◄──────┐
│    lat, lng     │       │
│    delivery_    │       │
│    radius_km    │       │
└────┬────────────┘       │
     │                    │
     │ 1                  │
     │                    │ N
     │ N          ┌───────┴──────────┐
     ▼            │  STORE_PRODUCT   │
┌─────────────────┤  (매장별 판매상품) │
│    PRODUCT      │──────────────────┤
│  (상품)          │◄─PK,FK store_id  │
├─────────────────┤◄─PK,FK product_id│
│ PK product_id   │    price         │
│    name         │    stock_qty     │
│    category     │    max_qty_per_  │
│    unit_weight_ │    order         │
│    kg           │                  │
└─────────────────┘                  │
                                     │
┌─────────────────────────────────┐  │
│         ORDER                   │  │
│        (주문)                    │  │
├─────────────────────────────────┤  │
│ PK order_id                     │  │
│ FK store_id ────────────────────┼──┘
│ FK user_id ─────────┐           │
│    origin_lat/lng   │           │
│    dest_lat/lng     │           │
│    total_weight_kg  │           │
│    total_amount     │           │
│    status           │           │
└────┬────────────────┘           │
     │                            │
     │ 1                          │ 1
     │                            │
     │ N                ┌─────────▼──────┐
     ▼                  │      USER      │
┌──────────────────┐    │    (사용자)     │
│  ORDER_ITEM      │    ├────────────────┤
│  (주문 항목)      │    │ PK user_id     │
├──────────────────┤    │    name        │
│ PK order_item_id │    │    phone       │
│ FK order_id      │    │    address     │
│ FK product_id    │    │    lat, lng    │
│    quantity      │    │    role        │
│    unit_price    │    └────────────────┘
└──────────────────┘
┌──────────────────────┐        ┌─────────────────┐
│    ROUTE_STOP        │        │     DRONE       │
│    (정류장)           │        │    (드론)        │
├──────────────────────┤        ├─────────────────┤
│ PK stop_id           │        │ PK drone_id     │
│ FK route_id          │◄───┐   │ FK store_id ────┼──┐
│    stop_seq          │    │   │    model        │  │
│    type              │    │   │    battery_     │  │
│    name              │    │   │    capacity     │  │
│    lat, lng          │    │   │    max_payload_ │  │
│    status            │    │   │    kg           │  │
│ FK store_id (opt)    │    │   │    status       │  │
│ FK user_id (opt)     │    │   └────┬────────────┘  │
│ FK order_id          │    │        │               │
└──────────────────────┘    │        │ 1             │
                            │        │               │
       ┌────────────────────┤        │ N             │
       │                    │        │               │
       │ N                  │ 1      │               │
       │                    │        │               │
       ▼                    ▼        ▼               │
┌──────────────────────────────────────┐             │
│            ROUTE                     │             │
│          (배송 경로)                  │             │
├──────────────────────────────────────┤             │
│ PK route_id                          │             │
│ FK drone_id                          │             │
│ FK store_id ─────────────────────────┼─────────────┘
│    status                            │
│    planned_start_at                  │
│    actual_start_at                   │
│    planned_total_distance_km         │
│    heuristic                         │
└───┬──────────────────────┬───────────┘
    │                      │
    │ 1                    │ 1
    │                      │
    │ N                    │ N
    ▼                      ▼
┌──────────────┐    ┌────────────┐
│ROUTE_POSITION│    │ FLIGHT_LOG │
│(드론 위치)    │    │ (비행 로그) │
├──────────────┤    ├────────────┤
│PK pos_id     │    │PK log_id   │
│FK route_id   │    │FK route_id │
│  lat, lng    │    │FK drone_id │
│  speed_mps   │    │  start_time│
│  battery_pct │    │  end_time  │
│  ts          │    │  distance  │
└──────────────┘    │  result    │
                    └────────────┘
```

### 7.2 주요 테이블 설명

#### store (매장)
- **역할**: 드론 배송 거점 (출발/복귀 지점)
- **인덱스**: (lat, lng) 공간 검색 최적화
- **특징**: `delivery_radius_km`로 배송 가능 반경 정의

#### store_product (매장별 판매 상품)
- **역할**: 매장마다 다른 가격/재고 관리
- **복합 키**: (store_id, product_id)
- **특징**: `stock_qty`가 실시간으로 증감

#### order (주문)
- **역할**: 고객의 주문서
- **상태**: CREATED → ASSIGNED → FULFILLED
- **특징**: `origin_*`는 Store, `dest_*`는 User

#### route (배송 경로)
- **역할**: 한 번의 드론 출격 임무
- **특징**: RouteStop들의 컨테이너 역할
- **상태**: PLANNED → LAUNCHED → IN_PROGRESS → COMPLETED

#### route_stop (정류장)
- **역할**: 경로상의 각 지점
- **타입**: PICKUP (픽업) → DROP들 (배송) → RETURN (복귀)
- **순서**: `stop_seq`로 방문 순서 보장

#### route_position (드론 위치)
- **역할**: 드론의 실시간 위치 기록
- **특징**: 2초마다 INSERT, `ts`로 시계열 데이터
- **인덱스**: (route_id, ts) 최신 위치 빠른 조회

### 7.3 인덱스 전략

```sql
-- 거리 기반 검색 최적화
CREATE INDEX ix_store_location ON store(lat, lng);

-- 상태별 빠른 조회
CREATE INDEX ix_dr_status ON drone(status);
CREATE INDEX ix_order_status ON `order`(status);
CREATE INDEX ix_route_status ON route(status);

-- 매장별 주문 조회
CREATE INDEX ix_order_store_status ON `order`(store_id, status);

-- 최신 위치 조회 최적화
CREATE INDEX ix_rp_route_ts ON route_position(route_id, ts);

-- 경로의 정류장 순서 조회
CREATE INDEX ix_rs_route_seq ON route_stop(route_id, stop_seq);
```

### 7.4 제약 조건

```sql
-- 위경도 범위 검증
CONSTRAINT chk_store_lat CHECK (lat BETWEEN -90 AND 90)
CONSTRAINT chk_store_lng CHECK (lng BETWEEN -180 AND 180)

-- 재고 음수 방지
CONSTRAINT chk_qty_nonneg CHECK (stock_qty >= 0)

-- 가격 음수 방지
CONSTRAINT chk_price_nonneg CHECK (price >= 0)

-- 드론 적재량 양수 보장
CONSTRAINT chk_payload_pos CHECK (max_payload_kg > 0)
```

---

## 8. 실시간 통신 구조

### 8.1 WebSocket 아키텍처

```
┌─────────────────┐
│   React Client  │
│                 │
│  ┌───────────┐  │
│  │  SockJS   │  │
│  │  Client   │  │
│  └─────┬─────┘  │
│        │        │
└────────┼────────┘
         │ WebSocket Connection
         │
         ▼
┌─────────────────────────────────┐
│   Spring Boot Backend           │
│                                 │
│  ┌───────────────────────────┐ │
│  │  WebSocketConfig          │ │
│  │  - enableSimpleBroker     │ │
│  │  - setApplicationDest...  │ │
│  └───────────────────────────┘ │
│               │                 │
│               ▼                 │
│  ┌───────────────────────────┐ │
│  │  Message Broker           │ │
│  │  Topic: /topic/route/{id} │ │
│  └───────┬───────────────────┘ │
│          │                     │
│          │ subscribe           │
│          │                     │
│  ┌───────▼───────────────────┐ │
│  │  DroneSimulatorService    │ │
│  │  - messagingTemplate      │ │
│  │  - convertAndSend()       │ │
│  └───────────────────────────┘ │
└─────────────────────────────────┘
```

### 8.2 STOMP 프로토콜

#### 연결 설정

**클라이언트 (JavaScript)**
```javascript
// SockJS + STOMP 라이브러리 필요
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('연결 성공:', frame);

    // 특정 경로 구독
    const routeId = 1;
    stompClient.subscribe('/topic/route/' + routeId, function(message) {
        const position = JSON.parse(message.body);
        console.log('드론 위치 업데이트:', position);

        // 지도에 마커 업데이트
        updateDroneMarker(position.lat, position.lng);
    });
});
```

**서버 (Spring Boot)**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트로 메시지를 보낼 때 prefix
        config.enableSimpleBroker("/topic");

        // 클라이언트에서 서버로 메시지를 보낼 때 prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000", "http://localhost:5173")
                .withSockJS();  // SockJS 폴백 지원
    }
}
```

#### 메시지 전송

**서버에서 클라이언트로 (브로드캐스트)**
```java
@Service
public class DroneSimulatorService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendPosition(Long routeId, DronePositionResponse position) {
        messagingTemplate.convertAndSend(
            "/topic/route/" + routeId,  // 토픽
            position                     // 메시지 (자동 JSON 변환)
        );
    }
}
```

### 8.3 메시지 흐름

```
1. 드론 시뮬레이터 시작
   DroneSimulatorService.simulateFlight(routeId=1)

2. 2초마다 위치 계산
   double[] position = GeoUtils.interpolate(...)

3. RoutePosition 저장
   routePositionRepository.save(rp)

4. WebSocket으로 브로드캐스트
   messagingTemplate.convertAndSend(
     "/topic/route/1",
     DronePositionResponse.from(rp)
   )

5. 구독 중인 모든 클라이언트에게 전달
   Client A ──┐
   Client B ──┼─► { lat: 37.5025, lng: 127.0325, ... }
   Client C ──┘

6. 클라이언트에서 지도 업데이트
   updateDroneMarker(position.lat, position.lng)
```

### 8.4 토픽 구조

| 토픽 | 설명 | 구독자 | 발행자 |
|------|------|--------|--------|
| `/topic/route/{routeId}` | 특정 경로의 드론 위치 | 해당 경로를 추적하는 사용자들 | DroneSimulatorService |

**예시**:
- `/topic/route/1`: Route ID=1의 드론 위치
- `/topic/route/2`: Route ID=2의 드론 위치

### 8.5 연결 해제 처리

```javascript
// 페이지 이탈 시 연결 해제
window.addEventListener('beforeunload', function() {
    if (stompClient !== null) {
        stompClient.disconnect(function() {
            console.log('WebSocket 연결 해제');
        });
    }
});
```

---

## 9. 배치 처리 및 스케줄링

### 9.1 스케줄러 설정

```java
@Configuration
@EnableScheduling  // ← 스케줄링 활성화
public class SchedulerConfig {
}
```

### 9.2 Cron 표현식

```
"0 */10 * * * *"
 │  │   │ │ │ │
 │  │   │ │ │ └─ 요일 (0-7, 0과 7은 일요일)
 │  │   │ │ └─── 월 (1-12)
 │  │   │ └───── 일 (1-31)
 │  │   └─────── 시 (0-23)
 │  └─────────── 분 (0-59) */10 = 매 10분마다
 └────────────── 초 (0-59)
```

**실행 시각**:
- 매 시간 0분, 10분, 20분, 30분, 40분, 50분
- 예: 09:00, 09:10, 09:20, ..., 23:50, 00:00

### 9.3 배치 처리 흐름

```
┌─────────────────────────────────────────────────────────┐
│              DeliveryScheduler                          │
│              @Scheduled(cron = "0 */10 * * * *")         │
└────────────────────┬────────────────────────────────────┘
                     │ 매 10분마다
                     ▼
┌─────────────────────────────────────────────────────────┐
│         DeliveryBatchService.processBatch()             │
├─────────────────────────────────────────────────────────┤
│  1. CREATED 상태 주문 조회                                │
│     └─ SELECT * FROM `order`                             │
│        WHERE status = 'CREATED'                          │
│                                                          │
│  2. 매장별 그룹화                                          │
│     └─ Map<StoreId, List<Order>>                         │
│                                                          │
│  3. 각 매장별 처리 (Loop)                                  │
│     ├─ 해당 매장의 대기 중인 드론 조회                      │
│     │  └─ SELECT * FROM drone                            │
│     │     WHERE store_id = ? AND status = 'IDLE'         │
│     │     LIMIT 1                                        │
│     │                                                    │
│     ├─ 경로 최적화 (TSP)                                  │
│     │  └─ RouteOptimizerService.optimizeRoute()         │
│     │                                                    │
│     ├─ Route 생성                                        │
│     │  └─ INSERT INTO route ...                         │
│     │                                                    │
│     ├─ RouteStop 생성 (PICKUP → DROP들 → RETURN)         │
│     │  └─ INSERT INTO route_stop ...                    │
│     │                                                    │
│     ├─ Order 상태 변경                                   │
│     │  └─ UPDATE `order`                                │
│     │     SET status = 'ASSIGNED' ...                   │
│     │                                                    │
│     ├─ Drone 상태 변경                                   │
│     │  └─ UPDATE drone SET status = 'IN_FLIGHT' ...     │
│     │                                                    │
│     └─ 드론 시뮬레이터 시작 (@Async)                       │
│        └─ DroneSimulatorService.simulateFlight()        │
│                                                          │
│  4. 트랜잭션 커밋                                          │
└─────────────────────────────────────────────────────────┘
```

### 9.4 동시성 제어

#### 문제: 같은 드론을 여러 경로에 할당?

```java
// ❌ 문제 상황
// 스레드 A: 드론#1 조회 → IDLE 확인
// 스레드 B: 드론#1 조회 → IDLE 확인 (동시)
// 스레드 A: 드론#1 → IN_FLIGHT 변경
// 스레드 B: 드론#1 → IN_FLIGHT 변경 (중복 할당!)
```

#### 해결책 1: Pessimistic Lock (비관적 락)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT d FROM Drone d WHERE d.status = 'IDLE'")
Optional<Drone> findFirstIdleDroneWithLock();

// SELECT ... FROM drone WHERE status = 'IDLE' FOR UPDATE
```

#### 해결책 2: Optimistic Lock (낙관적 락)

```java
@Entity
public class Drone {
    @Version
    private Long version;  // ← 버전 필드
}

// UPDATE drone SET status = ?, version = version + 1
// WHERE drone_id = ? AND version = ?
```

#### 해결책 3: Scheduled Lock (현재 구현)

```java
@Scheduled(cron = "0 */10 * * * *")
@SchedulerLock(name = "deliveryBatch", lockAtMostFor = "9m", lockAtLeastFor = "1m")
public void scheduleBatch() {
    // ShedLock 라이브러리 사용 시
    // 여러 서버가 있어도 하나만 실행됨
}
```

---

## 10. 에러 처리 전략

### 10.1 예외 계층 구조

```
Throwable
  └─ Exception
      ├─ RuntimeException
      │   ├─ BaseException (커스텀)
      │   │   ├─ 비즈니스 로직 예외
      │   │   └─ ErrorCode 포함
      │   │
      │   ├─ IllegalArgumentException
      │   ├─ IllegalStateException
      │   └─ ...
      │
      └─ Checked Exceptions
          ├─ IOException
          ├─ SQLException
          └─ ...
```

### 10.2 BaseException 구조

```java
@Getter
public class BaseException extends RuntimeException {
    private final ErrorCode errorCode;

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

// 사용 예시
if (store == null) {
    throw new BaseException(ErrorCode.STORE_NOT_FOUND);
}
```

### 10.3 ErrorCode Enum

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // format: (HTTP Status, 에러 코드, 메시지)

    // Common (C)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),

    // Store (S)
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "존재하지 않는 매장입니다."),

    // Product (P)
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "P003", "재고가 부족합니다."),

    // ... (총 60+ 에러 코드)

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

### 10.4 GlobalExceptionHandler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외
     */
    @ExceptionHandler(BaseException.class)
    protected ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException e) {
        log.error("BaseException: code={}, message={}",
            e.getErrorCode().getCode(), e.getMessage());

        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(BaseResponse.error(e.getErrorCode()));
    }

    /**
     * Validation 예외 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<BaseResponse<Void>> handleValidationException(
        MethodArgumentNotValidException e
    ) {
        String errorMessage = e.getBindingResult()
            .getAllErrors().get(0)
            .getDefaultMessage();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error(ErrorCode.INVALID_INPUT_VALUE, errorMessage));
    }

    /**
     * 그 외 모든 예외
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<BaseResponse<Void>> handleException(Exception e) {
        log.error("Unexpected Exception: ", e);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(BaseResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
```

### 10.5 예외 처리 흐름

```
Controller
   │
   │ @Valid 검증 실패
   ├─► MethodArgumentNotValidException
   │   └─► GlobalExceptionHandler
   │       └─► BaseResponse.error(C001, "수량은 1 이상이어야 합니다.")
   │
   │ 비즈니스 로직 호출
   ▼
Service
   │
   │ 재고 부족 발견
   └─► throw new BaseException(ErrorCode.PRODUCT_OUT_OF_STOCK)
       └─► GlobalExceptionHandler
           └─► BaseResponse.error(P003, "재고가 부족합니다.")
               └─► HTTP 400 Bad Request
```

---

## 11. 성능 최적화

### 11.1 N+1 문제 해결

#### 문제 상황
```java
// ❌ N+1 발생
List<Order> orders = orderRepository.findAll();

for (Order order : orders) {
    String storeName = order.getStore().getName();  // ← SELECT 쿼리 N번 추가 실행
}
```

#### 해결 방법: Fetch Join

```java
// ✅ Fetch Join 사용
@Query("SELECT o FROM Order o " +
       "JOIN FETCH o.store " +
       "JOIN FETCH o.user " +
       "WHERE o.status = :status " +
       "ORDER BY o.store.storeId, o.createdAt")
List<Order> findPendingOrdersWithStoreAndUser(@Param("status") OrderStatus status);

// 단 1번의 JOIN 쿼리로 모든 데이터 조회
```

### 11.2 Lazy Loading 활용

```java
@Entity
public class Route {
    @ManyToOne(fetch = FetchType.LAZY)  // ← 지연 로딩
    private Drone drone;

    @OneToMany(mappedBy = "route", fetch = FetchType.LAZY)
    private List<RouteStop> routeStops;
}

// drone, routeStops는 실제 사용할 때만 조회
```

### 11.3 인덱스 활용

```sql
-- 상태별 조회 빈번
CREATE INDEX ix_order_status ON `order`(status);

-- 매장별 + 상태별 조회
CREATE INDEX ix_order_store_status ON `order`(store_id, status);

-- 경로의 최신 위치 조회
CREATE INDEX ix_rp_route_ts ON route_position(route_id, ts);
```

**쿼리 실행 계획 확인**:
```sql
EXPLAIN SELECT * FROM `order` WHERE status = 'CREATED';
-- type: ref (인덱스 사용)
-- key: ix_order_status
```

### 11.4 배치 INSERT 최적화

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50  # ← 50개씩 묶어서 INSERT
        order_inserts: true
        order_updates: true
```

**효과**:
```sql
-- ❌ Before: N번의 INSERT
INSERT INTO route_position VALUES (...)
INSERT INTO route_position VALUES (...)
...

-- ✅ After: Batch INSERT
INSERT INTO route_position VALUES (...), (...), ..., (...)  -- 50개씩
```

### 11.5 Connection Pool 설정

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10     # 최대 연결 수
      minimum-idle: 5           # 최소 유휴 연결
      connection-timeout: 30000 # 30초
      idle-timeout: 600000      # 10분
```

---

## 12. 보안 고려사항

### 12.1 현재 구현 (데모용)

- ✅ CORS 설정: React 앱에서만 접근 가능
- ✅ SQL Injection 방지: JPA Parameterized Query
- ✅ Input Validation: `@Valid`, `@NotNull`, `@Min` 등
- ✅ Exception Handling: 스택 트레이스 숨김 (프로덕션)

### 12.2 실제 프로덕션 시 필요한 보안

#### 1. 인증/인가
```java
// Spring Security 도입
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/orders/**").authenticated()
                .requestMatchers("/api/stores/**").permitAll()
            )
            .oauth2Login()  // 또는 JWT
            .build();
    }
}
```

#### 2. API Rate Limiting
```java
// Bucket4j 라이브러리 사용
@RateLimiter(name = "orderApi", fallbackMethod = "rateLimitFallback")
public OrderResponse createOrder(OrderCreateRequest request) {
    // ...
}
```

#### 3. HTTPS 강제
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
```

#### 4. 민감 정보 암호화
```java
// Jasypt 라이브러리
spring:
  datasource:
    password: ENC(encrypted_password_here)
```

---

## 13. 테스트 전략

### 13.1 테스트 피라미드

```
          /\
         /  \        E2E Tests (5%)
        /────\
       /      \      Integration Tests (15%)
      /────────\
     /          \    Unit Tests (80%)
    /────────────\
```

### 13.2 Unit Test 예시

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreProductRepository storeProductRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("재고 부족 시 주문 생성 실패")
    void createOrder_OutOfStock_ThrowsException() {
        // given
        Store store = Store.builder()
            .storeId(1L)
            .isActive(true)
            .build();

        StoreProduct sp = StoreProduct.builder()
            .stockQty(5)  // 재고 5개
            .build();

        OrderItemRequest item = OrderItemRequest.builder()
            .productId(1L)
            .quantity(10)  // 10개 주문
            .build();

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(storeProductRepository.findById(...)).thenReturn(Optional.of(sp));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(BaseException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_OUT_OF_STOCK);
    }
}
```

### 13.3 Integration Test 예시

```java
@SpringBootTest
@AutoConfigureMockMvc
class StoreControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("주변 매장 조회 API 테스트")
    void getNearbyStores() throws Exception {
        mockMvc.perform(get("/api/stores")
                .param("lat", "37.5665")
                .param("lng", "126.9780")
                .param("radius", "5.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].storeId").exists());
    }
}
```

---

## 14. 배포 가이드

### 14.1 빌드

```bash
# JAR 파일 생성
./gradlew build

# 생성 위치
# build/libs/database-project-0.0.1-SNAPSHOT.jar
```

### 14.2 실행

```bash
# 개발 환경
java -jar build/libs/database-project-0.0.1-SNAPSHOT.jar

# 프로덕션 (프로파일 지정)
java -jar -Dspring.profiles.active=prod database-project-0.0.1-SNAPSHOT.jar
```

### 14.3 환경 변수

```bash
# 데이터베이스 설정
export DB_URL=jdbc:mysql://prod-db:3306/drone_delivery
export DB_USERNAME=app_user
export DB_PASSWORD=secure_password

# 실행
java -jar app.jar
```

### 14.4 Docker (선택사항)

**Dockerfile**
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/database-project-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml**
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DB_URL=jdbc:mysql://db:3306/drone_delivery
    depends_on:
      - db

  db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: drone_delivery
      MYSQL_ROOT_PASSWORD: root_password
    ports:
      - "3306:3306"
```

---

## 15. 결론

### 15.1 프로젝트 성과

✅ **도메인 중심 설계**로 유지보수성 확보
✅ **TSP 알고리즘**으로 실용적인 경로 최적화
✅ **WebSocket**으로 실시간 드론 추적 구현
✅ **Swagger**로 100% API 문서화
✅ **전역 예외 처리**로 일관된 에러 응답
✅ **스케줄러**로 자동화된 배송 배치 처리

### 15.2 향후 개선 방향

🔧 **알고리즘 고도화**: 2-opt, Genetic Algorithm
🔧 **실시간 재고 알림**: WebSocket으로 재고 부족 시 알림
🔧 **드론 배터리 관리**: 배터리 소모 예측 및 충전 스케줄링
🔧 **날씨 연동**: 기상청 API로 악천후 시 배송 중단
🔧 **통계 대시보드**: 배송 성공률, 평균 배송 시간 등
🔧 **Push 알림**: FCM으로 배송 상태 변경 알림

### 15.3 학습 포인트

- 도메인 주도 설계 (DDD) 실습
- TSP 알고리즘의 실용적 적용
- WebSocket을 통한 실시간 통신
- Spring Scheduler를 이용한 배치 처리
- JPA 성능 최적화 (N+1, Fetch Join, Index)
- Swagger를 통한 API 문서 자동화

---

## 부록

### A. 참고 자료

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [Haversine Formula](https://en.wikipedia.org/wiki/Haversine_formula)
- [Traveling Salesman Problem](https://en.wikipedia.org/wiki/Travelling_salesman_problem)

### B. 용어 사전

| 용어 | 설명 |
|------|------|
| TSP | Traveling Salesman Problem (순회 판매원 문제) |
| Haversine | 구면상의 두 점 사이 거리를 계산하는 공식 |
| STOMP | Simple Text Oriented Messaging Protocol |
| SockJS | WebSocket fallback 라이브러리 |
| CORS | Cross-Origin Resource Sharing |
| JPA | Java Persistence API |
| DTO | Data Transfer Object |
| Cron | 시간 기반 스케줄러 표현식 |

---

**문서 버전**: 1.0
**작성일**: 2025-11-20
**작성자**: Development Team
