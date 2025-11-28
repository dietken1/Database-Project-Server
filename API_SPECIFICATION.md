# Drone Delivery System API 명세서

## 개요
드론 멀티 배송 시스템의 REST API 명세서입니다.

**시스템 구성**
- **Backend**: Spring Boot 3.5.7 + Java 17
- **Database**: MySQL (JPA/Hibernate)
- **실시간 통신**: WebSocket (STOMP over SockJS)
- **API 문서**: Swagger/OpenAPI 3.0

## Base URL
```
http://localhost:8080/api
```

## 공통 응답 형식

본 API는 **RESTful** 원칙을 따르며, 성공/실패 여부는 **HTTP 상태 코드**로만 표현합니다.

### 성공 응답
성공 시 HTTP 상태 코드(200, 201 등)와 함께 **데이터만** 반환합니다.

```json
{
  "requestId": 1,
  "storeId": 1,
  "storeName": "편의점A",
  ...
}
```

### 에러 응답
실패 시 HTTP 상태 코드(400, 404, 500 등)와 함께 `ErrorResponse` 객체를 반환합니다.

```json
{
  "code": "STORE_NOT_FOUND",
  "message": "존재하지 않는 매장입니다.",
  "timestamp": "2024-01-15T14:30:00"
}
```

**ErrorResponse 필드**
- `code` (String): 에러 코드 (예: STORE_NOT_FOUND, PRODUCT_OUT_OF_STOCK)
- `message` (String): 에러 메시지 (한글)
- `timestamp` (String): 에러 발생 시각 (ISO 8601)

---

## 📑 목차

1. [사용자 (User) API](#1-사용자-user-api)
   - 1.1 [내 정보 조회](#11-내-정보-조회)
2. [주문 (Order) API](#2-주문-order-api)
   - 2.1 [주문 생성](#21-주문-생성)
   - 2.2 [주문 조회](#22-주문-조회)
3. [매장 (Store) API](#3-매장-store-api)
   - 3.1 [배달 가능한 매장 조회](#31-배달-가능한-매장-조회)
   - 3.2 [매장 검색](#32-매장-검색)
   - 3.3 [배송 정보 조회](#33-배송-정보-조회)
   - 3.4 [매장 카테고리 목록 조회](#34-매장-카테고리-목록-조회)
   - 3.5 [매장 상품 목록 조회](#35-매장-상품-목록-조회)
   - 3.6 [가게별 주문 목록 조회](#36-가게별-주문-목록-조회)
4. [배송 경로 (Route) API](#4-배송-경로-route-api)
5. [상태 코드 및 Enum](#5-상태-코드-및-enum)
6. [데모 시나리오](#6-데모-시나리오)

---

## 1. 사용자 (User) API

### 1.1 내 정보 조회

**GET** `/users/me`

요청 헤더의 userId를 통해 사용자의 이름, 주소, 위치 정보를 조회합니다.
매장 검색 시 사용자 위치를 자동으로 사용하거나, 배송지 정보를 표시할 때 활용됩니다.

**Request Headers**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| userId | Long | O | 사용자 ID | 1 |

**Response (200 OK)**
```json
{
  "name": "홍길동",
  "address": "서울시 강남구 테헤란로 123",
  "lat": 37.5665,
  "lng": 126.9780
}
```

**Response 필드 설명**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| name | String | O | 사용자명 | "홍길동" |
| address | String | O | 배송 주소 | "서울시 강남구 테헤란로 123" |
| lat | BigDecimal | O | 위도 | 37.5665 |
| lng | BigDecimal | O | 경도 | 126.9780 |

**Error Responses**
- `400 Bad Request`: 잘못된 요청
  - `USER_NOT_FOUND`: 존재하지 않는 사용자
- `500 Internal Server Error`: 서버 내부 오류

---

## 2. 주문 (Order) API

### 2.1 주문 생성

**POST** `/orders`

장바구니 내용을 기반으로 배송 요청을 생성합니다.
생성된 주문 ID만 반환하며, 상세 정보는 주문 조회 API를 통해 확인할 수 있습니다.

**Request Body**
```json
{
  "storeId": 1,
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 3,
      "quantity": 1
    }
  ],
  "note": "조심스럽게 배송해주세요."
}
```

**Request 필드 설명**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| storeId | Long | O | 매장 ID | 1 |
| userId | Long | O | 사용자 ID | 1 |
| items | Array | O | 주문 항목 목록 (최소 1개) | - |
| items[].productId | Long | O | 상품 ID | 1 |
| items[].quantity | Integer | O | 주문 수량 (최소 1) | 2 |
| note | String | X | 주문 메모 | "조심스럽게 배송해주세요." |

**Response (201 Created)**
```json
{
  "orderId": 1
}
```

**Response 필드 설명**

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| orderId | Long | 생성된 주문 ID | 1 |

> **참고**: 주문 상세 정보(총 금액, 항목 목록 등)는 `GET /api/orders/{orderId}` API로 조회할 수 있습니다.

**Error Responses**
- `400 Bad Request`: 잘못된 요청
  - `ORDER_ITEMS_EMPTY`: 주문 항목이 비어있음
  - `PRODUCT_OUT_OF_STOCK`: 재고 부족
  - `PRODUCT_EXCEED_MAX_QUANTITY`: 최대 주문 수량 초과
  - `ORDER_TOTAL_WEIGHT_EXCEEDED`: **주문 총 무게가 드론 적재 한계 초과** ⚠️
  - `STORE_OUT_OF_DELIVERY_RANGE`: **배송 가능 거리 초과** ⚠️
- `404 Not Found`: 리소스를 찾을 수 없음 (매장, 사용자, 상품 등)
  - `USER_NOT_FOUND`: 존재하지 않는 사용자
- `500 Internal Server Error`: 서버 내부 오류

> **중요**: 서버에서 무게 및 거리 검증을 수행하므로, 프론트엔드에서도 사전 검증을 구현하여 사용자 경험을 개선하세요.

---

### 2.2 주문 조회

**GET** `/orders/{orderId}`

주문 상세 정보를 조회합니다. 배송 완료 여부 확인에도 사용됩니다.

**Path Parameters**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| orderId | Long | O | 주문 ID | 1 |

**Response (200 OK)**
```json
{
  "orderId": 1,
  "storeId": 1,
  "storeName": "편의점A",
  "totalAmount": 15000,
  "status": "FULFILLED",
  "createdAt": "2024-01-15T14:00:00",
  "assignedAt": "2024-01-15T14:10:00",
  "completedAt": "2024-01-15T14:45:00",
  "items": [
    {
      "orderItemId": 1,
      "productId": 1,
      "productName": "아메리카노",
      "quantity": 2,
      "unitPrice": 3000,
      "subtotal": 6000
    }
  ],
  "note": "조심스럽게 배송해주세요.",
  "routeId": 5
}
```

**Response 필드 설명**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| routeId | Long | X | 배송 경로 ID (배송 할당 후 드론 위치 추적에 사용) | 5 |

**Response 필드 설명**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| orderId | Long | O | 주문 ID | 1 |
| storeId | Long | O | 매장 ID | 1 |
| storeName | String | O | 매장명 | "편의점A" |
| totalAmount | Integer | O | 총 금액 (원) | 15000 |
| status | String | O | 주문 상태 (CREATED, ASSIGNED, FULFILLED, CANCELED, FAILED) | "FULFILLED" |
| createdAt | DateTime | O | 주문 생성 시간 | "2024-01-15T14:00:00" |
| assignedAt | DateTime | X | 배송 할당 시간 | "2024-01-15T14:10:00" |
| completedAt | DateTime | X | 배송 완료 시간 | "2024-01-15T14:45:00" |
| items | Array | O | 주문 항목 목록 | - |
| items[].orderItemId | Long | O | 주문 항목 ID | 1 |
| items[].productId | Long | O | 상품 ID | 1 |
| items[].productName | String | O | 상품명 | "아메리카노" |
| items[].quantity | Integer | O | 주문 수량 | 2 |
| items[].unitPrice | Integer | O | 단가 (원) | 3000 |
| items[].subtotal | Integer | O | 소계 (원) | 6000 |
| note | String | X | 주문 메모 | "조심스럽게 배송해주세요." |
| routeId | Long | X | 배송 경로 ID (배송 할당 후 드론 추적에 사용) | 5 |

**배송 상태 확인 방법**
- `status` 필드 확인: `CREATED` (대기중) → `ASSIGNED` (배송중) → `FULFILLED` (완료)
- `completedAt` 필드가 null이 아니면 배송 완료
- `routeId`가 null이 아니면 배송이 할당된 상태 (드론 추적 가능)

**WebSocket 실시간 알림**
- 주문 완료 시 `/topic/order/{orderId}` 토픽으로 완료 알림이 전송됩니다
- 클라이언트는 주문 생성 후 해당 토픽을 구독하여 배송 완료 알림을 받을 수 있습니다
```javascript
stompClient.subscribe('/topic/order/1', (message) => {
  const data = JSON.parse(message.body);
  // { orderId: 1, status: "FULFILLED", message: "배송이 완료되었습니다!", completedAt: "..." }
  showToast(data.message); // 토스트 메시지 표시
});
```

**Error Responses**
- `404 Not Found`: 주문을 찾을 수 없음
- `500 Internal Server Error`: 서버 내부 오류

---

## 3. 매장 (Store) API

### 3.1 배달 가능한 매장 조회

**GET** `/stores`

사용자 위치를 기반으로 **배달 가능한** 매장 목록을 조회합니다.
각 매장은 자체적으로 배달 가능 거리(`deliveryRadiusKm`)를 가지고 있으며, 사용자 위치가 해당 거리 내에 있는 매장만 반환됩니다.

**Query Parameters**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| lat | BigDecimal | O | 사용자 위도 | 37.4979 |
| lng | BigDecimal | O | 사용자 경도 | 127.0276 |

> **동작 방식**:
> - 각 매장의 `deliveryRadiusKm` 내에 사용자가 있는지 확인
> - 배달 가능한 매장만 거리순으로 정렬하여 반환
> - 성능 최적화를 위해 내부적으로 최대 50km 반경 내에서만 검색
>
> **예시**:
> - 사용자 위치에서 1.5km 떨어진 매장A (deliveryRadiusKm: 2.0km) → ✅ 반환됨
> - 사용자 위치에서 3.0km 떨어진 매장B (deliveryRadiusKm: 2.0km) → ❌ 배달 불가로 제외됨

**Response (200 OK)**
```json
[
  {
    "storeId": 1,
    "name": "편의점A",
    "type": "CONVENIENCE",
    "phone": "02-1234-5678",
    "address": "서울시 강남구 테헤란로 123",
    "lat": 37.494095,
    "lng": 127.027610,
    "deliveryRadiusKm": 2.00,
    "distanceKm": 1.50
  }
]
```

**Error Responses**
- `400 Bad Request`: 잘못된 요청 (필수 파라미터 누락 등)

---

### 3.2 매장 검색 (NEW)

**GET** `/stores/search`

매장명으로 매장을 검색합니다 (부분 일치).
사용자 위치 정보를 제공하면 거리를 계산하고, **각 매장의 배달 가능 거리 내에 있는 매장만** 반환합니다.

**Query Parameters**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| name | String | O | 매장명 (부분 일치) | "편의점" |
| lat | BigDecimal | X | 사용자 위도 (거리 계산 및 배달 가능 여부 필터링용) | 37.4979 |
| lng | BigDecimal | X | 사용자 경도 (거리 계산 및 배달 가능 여부 필터링용) | 127.0276 |

> **참고**: `lat`/`lng`를 제공하지 않으면 모든 검색 결과가 반환되지만, 제공하면 배달 가능한 매장만 필터링됩니다.

**Response (200 OK)**
```json
[
  {
    "storeId": 1,
    "name": "편의점A 수원점",
    "type": "CONVENIENCE",
    "phone": "031-1234-5678",
    "address": "경기도 수원시 영통구...",
    "lat": 37.494095,
    "lng": 127.027610,
    "deliveryRadiusKm": 2.00,
    "distanceKm": 1.50
  }
]
```

---

### 3.3 배송 정보 조회 **(NEW)**

**GET** `/stores/{storeId}/delivery-info`

주문 전 검증에 필요한 배송 정보를 조회합니다.
**프론트엔드에서 이 정보를 사용하여 장바구니 무게 제한 및 배송 가능 여부를 실시간 검증할 수 있습니다.**

**Path Parameters**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| storeId | Long | O | 매장 ID | 1 |

**Query Parameters**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| lat | BigDecimal | X | 사용자 위도 (거리 및 배송 가능 여부 계산용) | 37.4979 |
| lng | BigDecimal | X | 사용자 경도 (거리 및 배송 가능 여부 계산용) | 127.0276 |

**Response (200 OK)**
```json
{
  "storeId": 1,
  "storeName": "편의점A",
  "deliveryRadiusKm": 2.00,
  "maxWeightKg": 5.000,
  "isDeliverable": true,
  "distanceKm": 1.50
}
```

**Response 필드 설명**

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| storeId | Long | 매장 ID | 1 |
| storeName | String | 매장명 | "편의점A" |
| deliveryRadiusKm | BigDecimal | 매장 배송 가능 반경 (km) | 2.00 |
| maxWeightKg | BigDecimal | 시스템 드론 최대 적재 무게 (kg) | 5.000 |
| isDeliverable | Boolean | 배송 가능 여부 (위치 제공 시) | true |
| distanceKm | BigDecimal | 매장-사용자 간 거리 (위치 제공 시) | 1.50 |

> **프론트엔드 활용 예시**:
> - `maxWeightKg`로 장바구니 무게 제한 검증
> - `deliveryRadiusKm`과 `distanceKm`으로 배송 가능 여부 표시
> - `isDeliverable=false`면 "배송 불가 지역입니다" 경고 표시

**Error Responses**
- `404 Not Found`: 존재하지 않는 매장
- `400 Bad Request`: 운영 중이지 않은 매장

---

### 3.4 매장 카테고리 목록 조회

**GET** `/stores/{storeId}/categories`

특정 매장에서 판매 중인 상품의 카테고리 목록을 조회합니다.

**Path Parameters**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| storeId | Long | O | 매장 ID | 1 |

**Response (200 OK)**
```json
[
  {
    "category": "음료"
  },
  {
    "category": "스낵"
  }
]
```

**Error Responses**
- `404 Not Found`: 존재하지 않는 매장
- `400 Bad Request`: 운영 중이지 않은 매장

---

### 3.5 매장 상품 목록 조회

**GET** `/stores/{storeId}/products`

특정 매장의 상품 목록을 조회합니다.
**카테고리 파라미터가 제공되면 해당 카테고리의 상품만, 제공되지 않으면 모든 판매 중인 상품을 조회합니다.**

**Path Parameters**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| storeId | Long | O | 매장 ID | 1 |

**Query Parameters**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| category | String | X | 카테고리 필터 | "음료" |

**Response (200 OK)**
```json
[
  {
    "productId": 1,
    "name": "초코우유",
    "category": "음료",
    "unitWeightKg": 0.200,
    "price": 2500,
    "stockQty": 50,
    "maxQtyPerOrder": 10
  }
]
```

**Error Responses**
- `404 Not Found`: 존재하지 않는 매장
- `400 Bad Request`: 운영 중이지 않은 매장

---

### 3.6 가게별 주문 목록 조회

**GET** `/stores/{storeId}/orders`

특정 가게로 들어온 주문 목록을 조회합니다. 점주가 자신의 가게에 쌓인 주문들을 확인하고 배송할 주문을 선택할 때 사용합니다.
`status` 파라미터로 특정 상태의 주문만 필터링할 수 있습니다.

**Path Parameters**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| storeId | Long | O | 가게 ID | 1 |

**Query Parameters**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| status | String | X | 주문 상태 필터 (CREATED, ASSIGNED, FULFILLED, CANCELED, FAILED) | CREATED |

**Response (200 OK)**
```json
[
  {
    "orderId": 1,
    "storeId": 1,
    "storeName": "편의점A",
    "totalAmount": 15000,
    "status": "CREATED",
    "createdAt": "2024-01-15T14:00:00",
    "assignedAt": null,
    "completedAt": null,
    "items": [
      {
        "orderItemId": 1,
        "productId": 1,
        "productName": "아메리카노",
        "quantity": 2,
        "unitPrice": 3000,
        "subtotal": 6000
      }
    ],
    "note": "조심스럽게 배송해주세요.",
    "routeId": null
  },
  {
    "orderId": 2,
    "storeId": 1,
    "storeName": "편의점A",
    "totalAmount": 8000,
    "status": "CREATED",
    "createdAt": "2024-01-15T14:05:00",
    "assignedAt": null,
    "completedAt": null,
    "items": [
      {
        "orderItemId": 2,
        "productId": 3,
        "productName": "초코우유",
        "quantity": 2,
        "unitPrice": 2500,
        "subtotal": 5000
      }
    ],
    "note": null,
    "routeId": null
  }
]
```

**Response 필드 설명**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| orderId | Long | O | 주문 ID | 1 |
| storeId | Long | O | 매장 ID | 1 |
| storeName | String | O | 매장명 | "편의점A" |
| totalAmount | Integer | O | 총 금액 (원) | 15000 |
| status | String | O | 주문 상태 | "CREATED" |
| createdAt | DateTime | O | 주문 생성 시간 | "2024-01-15T14:00:00" |
| assignedAt | DateTime | X | 배송 할당 시간 | null |
| completedAt | DateTime | X | 배송 완료 시간 | null |
| items | Array | O | 주문 항목 목록 | - |
| items[].orderItemId | Long | O | 주문 항목 ID | 1 |
| items[].productId | Long | O | 상품 ID | 1 |
| items[].productName | String | O | 상품명 | "아메리카노" |
| items[].quantity | Integer | O | 주문 수량 | 2 |
| items[].unitPrice | Integer | O | 단가 (원) | 3000 |
| items[].subtotal | Integer | O | 소계 (원) | 6000 |
| note | String | X | 주문 메모 | "조심스럽게 배송해주세요." |
| routeId | Long | X | 배송 경로 ID (배송 할당 후) | null |

**사용 예시**

```http
# 전체 주문 조회
GET /api/stores/1/orders

# 배송 대기 중인 주문만 조회 (점주가 배송 시작할 주문 선택용)
GET /api/stores/1/orders?status=CREATED

# 배송 완료된 주문만 조회
GET /api/stores/1/orders?status=FULFILLED
```

**Error Responses**
- `404 Not Found`: 가게를 찾을 수 없음
- `500 Internal Server Error`: 서버 내부 오류

---

## 4. 배송 경로 (Route) API

### 4.1 경로 상세 조회

**GET** `/routes/{routeId}`

특정 배송 경로의 상세 정보를 조회합니다. 경로에 포함된 모든 정류장 정보를 함께 반환합니다.

**Path Parameters**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| routeId | Long | O | 경로 ID | 1 |

**Response (200 OK)**
```json
{
  "routeId": 1,
  "droneId": 1,
  "droneModel": "DJI Phantom 4",
  "storeId": 1,
  "storeName": "편의점A",
  "status": "LAUNCHED",
  "plannedStartAt": "2024-01-15T14:00:00",
  "plannedEndAt": "2024-01-15T15:00:00",
  "actualStartAt": "2024-01-15T14:05:00",
  "actualEndAt": null,
  "plannedTotalDistanceKm": 12.50,
  "plannedTotalPayloadKg": 3.500,
  "heuristic": "Nearest Neighbor",
  "stops": [
    {
      "stopId": 1,
      "stopSeq": 1,
      "type": "PICKUP",
      "name": "편의점A",
      "lat": 37.123456,
      "lng": 127.123456,
      "status": "DEPARTED",
      "plannedArrivalAt": "2024-01-15T14:00:00",
      "plannedDepartureAt": "2024-01-15T14:02:00",
      "actualArrivalAt": "2024-01-15T14:05:00",
      "actualDepartureAt": "2024-01-15T14:07:00",
      "payloadDeltaKg": 3.500
    },
    {
      "stopId": 2,
      "stopSeq": 2,
      "type": "DROP",
      "name": "김철수",
      "lat": 37.123456,
      "lng": 127.123456,
      "status": "PENDING",
      "plannedArrivalAt": "2024-01-15T14:25:00",
      "plannedDepartureAt": "2024-01-15T14:27:00",
      "actualArrivalAt": null,
      "actualDepartureAt": null,
      "payloadDeltaKg": -1.500
    }
  ],
  "note": "Batch processed at 2024-01-15T14:00:00"
}
```

**Error Responses**
- `404 Not Found`: 존재하지 않는 배송 경로

---

### 4.2 드론 현재 위치 조회

**GET** `/routes/{routeId}/current-position`

특정 배송 경로의 드론 현재 위치를 실시간으로 조회합니다. 위도, 경도, 속도, 배터리 잔량 등의 정보를 포함합니다.

**Path Parameters**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| routeId | Long | O | 경로 ID | 1 |

**Response (200 OK)**
```json
{
  "routeId": 1,
  "lat": 37.123456,
  "lng": 127.123456,
  "speedMps": 8.33,
  "batteryPct": 85.50,
  "ts": "2024-01-15T14:30:00"
}
```

**Error Responses**
- `404 Not Found`: 존재하지 않는 배송 경로 또는 위치 정보 없음

---

### 4.3 진행 중인 배송 목록 조회

**GET** `/routes/active`

현재 진행 중인 모든 배송 경로 목록을 조회합니다. LAUNCHED 상태인 경로들을 반환합니다.

**Response (200 OK)**
```json
[
  {
    "routeId": 1,
    "droneId": 1,
    "droneModel": "DJI Phantom 4",
    "storeId": 1,
    "storeName": "편의점A",
    "status": "LAUNCHED",
    "plannedStartAt": "2024-01-15T14:00:00",
    "plannedEndAt": "2024-01-15T15:00:00",
    "actualStartAt": "2024-01-15T14:05:00",
    "actualEndAt": null,
    "plannedTotalDistanceKm": 12.50,
    "plannedTotalPayloadKg": 3.500,
    "heuristic": "Nearest Neighbor",
    "stops": [...],
    "note": "Batch processed at 2024-01-15T14:00:00"
  }
]
```

---

### 4.4 배송 시작

**POST** `/routes/start-delivery`

점주가 선택한 주문들(최대 3개)을 배송 시작합니다. 모든 주문은 같은 매장이어야 하며, CREATED 상태여야 합니다.

**처리 과정**
1. 요청된 주문 ID로 주문 조회
2. 모든 주문이 CREATED 상태인지 확인
3. 모든 주문이 같은 매장인지 확인
4. 사용 가능한 드론 확인
5. **드론의 최대 무게 및 배터리 검증** (초과 시 예외 발생)
6. 주문 시간순 정렬 및 최적 경로 탐색 (Nearest Neighbor 휴리스틱)
7. 경로 생성 및 드론 시뮬레이터 자동 시작

**Request Body**
```json
{
  "orderIds": [1, 2, 3]
}
```

**Request 필드 설명**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| orderIds | Array<Long> | O | 배송할 주문 ID 리스트 (최소 1개, 최대 3개) | [1, 2, 3] |

**Response (200 OK)**
```json
"배송이 성공적으로 시작되었습니다."
```

**Error Responses**
- `400 Bad Request`: 잘못된 요청
  - 주문 ID가 비어있음
  - 주문 개수가 1개 미만 또는 3개 초과
  - `PayloadExceededException`: **드론의 최대 적재량 초과** (예: "드론의 최대 적재량을 초과했습니다. 총 무게: 12.0kg, 최대 적재량: 10.0kg")
  - `BatteryInsufficientException`: **배터리 용량 부족** (예: "배터리 용량이 부족합니다. 예상 거리: 18.00km, 최대 거리: 16.00km")
  - 주문 상태가 CREATED가 아님 (이미 처리되었거나 취소됨)
  - 모든 주문이 같은 매장이 아님
- `404 Not Found`: 주문 또는 드론을 찾을 수 없음
  - 존재하지 않는 주문 ID
  - 해당 매장에 사용 가능한 드론이 없음

> **중요**:
> - 드론 최대 적재량: 드론마다 다를 수 있음 (일반적으로 5~10kg)
> - 배터리 제한: 최대 16km (배터리 20% 여유 확보)
> - 배터리 소모: km당 5% (총 20km까지 가능하나 안전 마진 고려)

---

### 4.5 배송 배치 처리 (전체 자동)

**POST** `/routes/batch-delivery`

현재까지 대기 중인 모든 주문을 자동으로 수집하여 배송을 시작합니다. 스케줄러 또는 관리자용 API입니다.

**처리 과정**
1. 대기 중인 주문들을 수집 (CREATED 상태)
2. 주문 시간순으로 정렬
3. 매장별로 그룹화
4. 각 매장의 사용 가능한 드론 확인
5. 드론의 최대 무게 및 배터리를 고려하여 할당 가능한 주문 선택
6. 각 그룹별로 최적 경로 탐색 (Nearest Neighbor 휴리스틱)
7. 경로 생성 및 드론 시뮬레이터 자동 시작

**Request**

요청 본문 없음 (Request Body 없음)

**Response (200 OK)**
```json
"배송 배치 처리가 완료되었습니다."
```

**Error Responses**
- `400 Bad Request`: 사용 가능한 드론이 없거나 대기 중인 주문이 없음

---

## 5. 상태 코드 및 Enum

### HTTP 상태 코드

| 상태 코드 | 설명 |
|-----------|------|
| 200 OK | 요청 성공 |
| 201 Created | 리소스 생성 성공 |
| 400 Bad Request | 잘못된 요청 (유효성 검증 실패, 잘못된 파라미터 등) |
| 404 Not Found | 요청한 리소스를 찾을 수 없음 |
| 500 Internal Server Error | 서버 내부 오류 |

### 주문 상태 (Order Status)

| 상태 | 설명 |
|------|------|
| CREATED | 주문 생성됨 (배송 대기 중) |
| ASSIGNED | 드론에 배송 할당됨 (배송 진행 중) |
| FULFILLED | 배송 완료 |
| CANCELED | 주문 취소됨 |
| FAILED | 배송 실패 |

### 경로 상태 (Route Status)

| 상태 | 설명 |
|------|------|
| PLANNED | 경로 계획 완료 (출발 전) |
| LAUNCHED | 배송 시작됨 |
| IN_PROGRESS | 배송 진행 중 |
| COMPLETED | 배송 완료 |
| ABORTED | 배송 중단됨 |

### 정류장 타입 (Stop Type)

| 타입 | 설명 |
|------|------|
| PICKUP | 픽업 지점 (매장에서 상품 적재) |
| DROP | 드롭 지점 (고객 주소에 배송) |
| RETURN | 복귀 지점 (매장으로 복귀) |

### 정류장 상태 (Stop Status)

| 상태 | 설명 |
|------|------|
| PENDING | 대기 중 |
| ARRIVED | 도착 완료 |
| DEPARTED | 출발 완료 |
| SKIPPED | 스킵됨 |
| FAILED | 실패 |

### 매장 유형 (Store Type)

| 유형 | 설명 |
|------|------|
| CONVENIENCE | 편의점 |
| PHARMACY | 약국 |
| OTHER | 기타 |

---

## 6. 데모 시나리오

이 섹션에서는 드론 배송 시스템의 전체 플로우를 클라이언트 요청과 서버 로직을 혼합하여 설명합니다.

### 📱 시나리오 1: 고객의 주문부터 배송 완료까지

#### Step 1: 사용자 위치 기반 매장 검색

**클라이언트 동작**
- 사용자가 앱을 실행하면 위치 권한을 요청
- GPS로 현재 위치(위도/경도) 획득

**API 요청**
```http
GET /api/stores?lat=37.4979&lng=127.0276
```
> 각 매장의 배달 가능 거리(`deliveryRadiusKm`) 내에 사용자가 있는 매장만 조회됩니다.

**서버 로직**
1. `StoreController.getNearbyStores()` 호출
2. `StoreService.getStoresNearby()` 실행
3. `StoreRepository.findStoresWithinRadius()` - Haversine 공식으로 반경 내 매장 검색
4. 각 매장의 거리 계산 (`GeoUtils.calculateDistance()`)
5. 거리순으로 정렬하여 반환

**클라이언트 화면**
- 가까운 매장 목록 표시 (거리순)
- 각 매장: 이름, 타입, 주소, 거리 표시

---

#### Step 2: 매장 선택 및 배송 정보 조회

**클라이언트 동작**
- 사용자가 매장 선택 (예: "편의점A 수원점")
- **배송 가능 여부 및 무게 제한 확인 필요**

**API 요청**
```http
GET /api/stores/1/delivery-info?lat=37.4979&lng=127.0276
```

**서버 로직**
1. `StoreController.getDeliveryInfo()` 호출
2. `StoreService.getDeliveryInfo()` 실행
3. 매장 존재 및 활성 상태 확인
4. `DroneRepository.findMinMaxPayloadKg()` - 시스템 드론 최대 무게 조회 (기본 5kg)
5. 사용자 위도/경도가 제공되면 거리 계산 및 배송 가능 여부 판단
6. `DeliveryInfoResponse` 반환

**클라이언트 화면**
- 배송 가능 여부 표시: "✅ 배송 가능 (거리: 1.5km)" 또는 "❌ 배송 불가 (거리 초과)"
- 장바구니 최대 무게 제한 표시: "최대 5.0kg까지 담을 수 있습니다"
- 현재 장바구니 무게: "현재 0kg / 5.0kg"

---

#### Step 3: 카테고리 조회

**API 요청**
```http
GET /api/stores/1/categories
```

**서버 로직**
1. `StoreController.getCategories()` 호출
2. `StoreService.getCategories(storeId=1)` 실행
3. 매장 존재 및 활성 상태 확인
4. `StoreProductRepository.findCategoriesByStoreId()` - JPQL 쿼리로 중복 제거된 카테고리 조회
5. 카테고리 목록 반환

**클라이언트 화면**
- 카테고리 목록 표시 (예: 음료, 스낵, 라면, 빵, 도시락)

---

#### Step 4: 카테고리별 상품 조회

**클라이언트 동작**
- 사용자가 카테고리 선택 (예: "음료")

**API 요청**
```http
GET /api/stores/1/products?category=음료
```

**서버 로직**
1. `StoreController.getProducts()` 호출
2. `StoreService.getProducts(storeId=1, category="음료")` 실행
3. 매장 존재 및 활성 상태 확인
4. `StoreProductRepository.findActiveProductsByStoreIdAndCategory()` - JOIN FETCH로 성능 최적화
5. 상품 목록을 `ProductResponse` DTO로 변환

**클라이언트 화면**
- 음료 카테고리 상품 목록 표시
- 각 상품: 이름, 가격, 재고, 무게, 최대 주문 수량 표시

---

#### Step 5: 장바구니 담기 및 주문 생성

**클라이언트 동작**
- 사용자가 상품을 장바구니에 추가 (예: 아메리카노 2개, 초코우유 1개)
- **실시간 무게 검증**: 상품 추가 시 현재 무게 + 추가 무게가 `maxWeightKg` 초과하면 경고
- "주문하기" 버튼 클릭

**API 요청**
```http
POST /api/orders
Content-Type: application/json

{
  "storeId": 1,
  "userId": 5,
  "items": [
    {"productId": 1, "quantity": 2},
    {"productId": 2, "quantity": 1}
  ],
  "note": "빨리 배송 부탁드려요"
}
```

**서버 로직**
1. `OrderController.createOrder()` 호출
2. `OrderService.createOrder()` 실행
3. **검증 단계**:
   - 매장 존재 및 활성 상태 확인
   - 사용자 존재 확인
   - 각 상품의 재고 확인 (`StoreProduct` 조회)
   - 주문 수량이 `maxQtyPerOrder` 이하인지 확인
   - **드론 최대 적재 무게 검증** (초과 시 `ORDER_TOTAL_WEIGHT_EXCEEDED`)
   - **배송 가능 거리 검증** (초과 시 `STORE_OUT_OF_DELIVERY_RANGE`)
4. **주문 생성**:
   - `Order` 엔티티 생성 (상태: CREATED)
   - 각 상품별로 `OrderItem` 생성
   - 총 무게 계산 (`unitWeightKg × quantity` 합산)
   - 총 금액 계산 (`price × quantity` 합산)
5. **재고 차감**:
   - 각 상품의 `StoreProduct.stockQty` 감소
6. **트랜잭션 커밋** 후 `OrderCreateResponse` 반환

**클라이언트 화면**
- 주문 완료 화면 표시
- 주문 번호, 총 금액, 배송 예상 시간 표시

---

#### Step 6: 주문 상태 확인 (폴링)

**클라이언트 동작**
- 5초마다 주문 상태 폴링

**API 요청**
```http
GET /api/orders/1
```

**서버 로직**
1. `OrderController.getOrder()` 호출
2. `OrderService.getOrder(orderId=1)` 실행
3. `OrderRepository.findById()` 조회
4. `OrderResponse` 변환하여 반환

**클라이언트 화면**
- 주문 상태에 따라 UI 업데이트:
  - `CREATED`: "주문 접수 완료, 배송 준비 중..."
  - `ASSIGNED`: "드론이 배송 중입니다!"
  - `FULFILLED`: "배송이 완료되었습니다!"

---

### 🏪 시나리오 2: 점주의 배송 시작 및 실시간 모니터링

#### Step 1: 점주가 대기 중인 주문 목록 조회

**클라이언트 동작**
- 점주가 관리자 페이지 접속
- 자신의 가게로 들어온 주문 목록 확인

**API 요청**
```http
GET /api/stores/1/orders?status=CREATED
```

**서버 로직**
1. `StoreController.getStoreOrders()` 호출
2. `OrderService.getStoreOrders(storeId=1, status=CREATED)` 실행
3. 매장 존재 확인
4. `OrderRepository.findByStoreStoreIdAndStatus()` - 해당 가게의 CREATED 상태 주문 조회
5. 각 주문을 `OrderResponse`로 변환하여 반환

**클라이언트 화면**
- 대기 중인 주문 목록 테이블
- 각 주문: 주문번호, 주문시간, 상품목록, 총금액 표시
- 체크박스로 배송할 주문 선택 (최대 3개)

---

#### Step 2: 점주가 배송 시작 요청

**클라이언트 동작**
- 배송할 주문 선택 (최대 3개)
- "배송 시작" 버튼 클릭

**API 요청**
```http
POST /api/routes/start-delivery
Content-Type: application/json

{
  "orderIds": [1, 2, 3]
}
```

**서버 로직 (복잡한 비즈니스 로직)**

1. **`RouteController.startDelivery()` 호출**
2. **`DeliveryBatchService.processSelectedOrders()` 실행** - 선택된 주문 처리
3. **주문 조회 및 검증**:
   - `OrderRepository.findById()` - 각 주문 ID로 주문 조회
   - 모든 주문이 CREATED 상태인지 확인 (아니면 예외)
   - 모든 주문이 같은 매장인지 확인 (아니면 예외)
4. **사용 가능한 드론 조회**:
   - `DroneRepository.findFirstByStoreAndStatus(store, IDLE)` - 해당 매장의 IDLE 드론
5. **무게 및 거리 검증**:
   ```
   총무게 = 주문들의 총 무게 합산
   IF 총무게 > 드론최대적재량:
     THROW PayloadExceededException
   END IF

   예상거리 = 매장 → 각 배송지 → 매장 거리 합산
   IF 예상거리 > 16km:
     THROW BatteryInsufficientException
   END IF
   ```
6. **주문 시간순 정렬**:
   - `createdAt` 기준으로 정렬 (먼저 주문한 고객 우선)
6. **각 배송 그룹별로 최적 경로 생성**:
   - `RouteOptimizerService.optimizeRoute()` 호출
   - **Nearest Neighbor 휴리스틱 알고리즘**:
     ```
     1. 시작점 = 매장 위치
     2. 미방문 주문들의 배송 위치 리스트 생성
     3. 현재위치 = 시작점

     WHILE 미방문 위치가 있음:
       가장가까운위치 = NULL
       최소거리 = Infinity

       FOR EACH 미방문위치:
         거리 = GeoUtils.calculateDistance(현재위치, 미방문위치)
         IF 거리 < 최소거리:
           최소거리 = 거리
           가장가까운위치 = 미방문위치
         END IF
       END FOR

       경로.추가(가장가까운위치)
       미방문위치.제거(가장가까운위치)
       현재위치 = 가장가까운위치
       총거리 += 최소거리
     END WHILE

     4. 마지막에 매장으로 복귀 경로 추가
     5. 총 비행 시간 계산 = 총거리 / 드론속도(30km/h)
     ```
7. **Route 및 RouteStop 엔티티 생성**:
   - `Route` 생성 (상태: PLANNED)
   - 각 배송지마다 `RouteStop` 생성:
     - `stopSequence` 1: PICKUP (매장) - payloadDelta = +총무게
     - `stopSequence` 2~N: DROP (고객 주소) - payloadDelta = -주문무게
     - `stopSequence` N+1: RETURN (매장) - payloadDelta = 0
8. **주문 상태 업데이트**:
   - 각 주문의 상태를 CREATED → ASSIGNED로 변경
   - `assignedAt` 타임스탬프 기록
9. **드론 시뮬레이터 시작**:
   - `DroneSimulatorService.simulateFlight(routeId)` 비동기 호출
   - 별도 스레드에서 시뮬레이션 시작

**클라이언트 화면**
- "배송이 성공적으로 시작되었습니다!" 메시지 표시
- 진행 중인 배송 목록 화면으로 이동

---

#### Step 3: 드론 시뮬레이터 동작

**서버 로직 (비동기 실행)**

`DroneSimulatorService.simulateFlight(routeId)` 내부 동작:

1. **Route 및 RouteStops 조회**:
   - `RouteRepository.findByIdWithDetails(routeId)` - JOIN FETCH로 한 번에 조회
2. **Route 상태 변경**:
   - PLANNED → LAUNCHED
   - `actualStartAt` = 현재시간
3. **각 정류장(RouteStop)을 순회하며 시뮬레이션**:
   ```
   총배터리사용량 = 0
   총이동거리 = 0

   FOR i = 0 TO stops.length - 1:
     현재Stop = stops[i]

     IF i == 0:
       시작위치 = 매장위치
     ELSE:
       시작위치 = stops[i-1].위치
     END IF

     끝위치 = 현재Stop.위치
     구간거리 = GeoUtils.calculateDistance(시작위치, 끝위치)
     이동시간 = (구간거리 * 1000m) / 드론속도(8.33m/s)
     단계수 = ceil(이동시간 / 2초)

     // 선형 보간으로 드론 이동 시뮬레이션
     FOR step = 0 TO 단계수:
       진행비율 = step / 단계수
       현재위치 = GeoUtils.interpolate(시작위치, 끝위치, 진행비율)
       배터리잔량 = 100 - (총이동거리 * 5)

       // RoutePosition 저장 (DB)
       RoutePosition.create(
         route = route,
         lat = 현재위치.lat,
         lng = 현재위치.lng,
         speed = 8.33 m/s,
         battery = 배터리잔량,
         timestamp = 현재시간
       )

       // WebSocket으로 실시간 브로드캐스트
       messagingTemplate.convertAndSend(
         "/topic/route/" + routeId,
         {
           routeId: routeId,
           lat: 현재위치.lat,
           lng: 현재위치.lng,
           speed: 30 km/h,
           battery: 배터리잔량,
           timestamp: 현재시간
         }
       )

       Thread.sleep(2000ms) // 2초 대기
     END FOR

     // Stop 도착 처리
     현재Stop.상태 = ARRIVED
     현재Stop.actualArrivalAt = 현재시간

     IF 현재Stop.type == DROP:
       Thread.sleep(3000ms) // 배송 시뮬레이션 (3초 대기)
       현재Stop.상태 = DEPARTED
       현재Stop.actualDepartureAt = 현재시간

       // 해당 주문 완료 처리
       해당주문.상태 = FULFILLED
       해당주문.completedAt = 현재시간
     END IF

     총이동거리 += 구간거리
   END FOR
   ```
4. **Route 완료 처리**:
   - LAUNCHED → COMPLETED
   - `actualEndAt` = 현재시간
5. **FlightLog 생성**:
   - 비행 시작/종료 시간, 총 거리, 배터리 사용량 기록
   - 비행 결과: SUCCESS

---

#### Step 4: 점주의 실시간 모니터링

**클라이언트 동작 (WebSocket 연결)**

```javascript
// WebSocket 연결
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // 특정 경로 구독
  stompClient.subscribe('/topic/route/1', (message) => {
    const position = JSON.parse(message.body);

    // 지도에 드론 위치 업데이트
    updateDroneMarker(position.lat, position.lng);

    // 배터리 게이지 업데이트
    updateBatteryGauge(position.battery);

    // 속도 표시
    updateSpeed(position.speed);
  });
});
```

**API 요청 (폴링 방식)**

```http
GET /api/routes/1/current-position
```

**서버 로직**
1. `RouteController.getCurrentPosition()` 호출
2. `RouteService.getCurrentPosition(routeId=1)` 실행
3. `RoutePositionRepository.findTopByRouteOrderByTsDesc()` - 최신 위치 조회
4. `DronePositionResponse` 반환

**클라이언트 화면**
- 지도에 드론 아이콘 표시 (실시간 이동)
- 배터리 잔량 게이지
- 현재 속도 표시
- 다음 목적지까지 거리 및 예상 시간

---

#### Step 5: 진행 중인 배송 목록 조회

**API 요청**
```http
GET /api/routes/active
```

**서버 로직**
1. `RouteController.getActiveRoutes()` 호출
2. `RouteService.getActiveRoutes()` 실행
3. `RouteRepository.findByStatusIn([LAUNCHED])` - 진행 중인 경로만 조회
4. 각 Route를 `RouteResponse`로 변환 (stops 포함)

**클라이언트 화면**
- 진행 중인 배송 목록 테이블
- 각 배송: 드론 모델, 매장명, 출발 시간, 배송지 개수, 진행 상태

---

### 🔍 시나리오 3: 고급 검색 기능 활용

#### 매장명으로 검색

**클라이언트 동작**
- 사용자가 검색창에 "편의점" 입력

**API 요청**
```http
GET /api/stores/search?name=편의점&lat=37.4979&lng=127.0276
```

**서버 로직**
1. `StoreController.searchStores()` 호출
2. `StoreService.searchStoresByName()` 실행
3. `StoreRepository.findByNameContainingAndIsActiveTrue("편의점")` - LIKE 검색
4. 위도/경도가 제공되면 각 매장의 거리 계산
5. 매장명 순으로 정렬하여 반환

---

## 7. 주요 알고리즘 및 기술 상세

### 7.1 Haversine 공식 (거리 계산)

사용자 위치와 매장 간의 거리를 계산하는 알고리즘입니다.

```java
// GeoUtils.calculateDistance()
public static double calculateDistance(
    double lat1, double lng1,
    double lat2, double lng2
) {
    final double R = 6371; // 지구 반경 (km)

    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
               Math.cos(Math.toRadians(lat1)) *
               Math.cos(Math.toRadians(lat2)) *
               Math.sin(dLng / 2) * Math.sin(dLng / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c; // km
}
```

---

### 7.2 Nearest Neighbor TSP 휴리스틱

배송 경로 최적화에 사용되는 알고리즘입니다.

**시간 복잡도**: O(n²)
**특징**: 항상 최적해는 아니지만, 실시간 처리에 적합한 빠른 근사 알고리즘

```
INPUT: 배송지 리스트, 시작점(매장)
OUTPUT: 최적화된 방문 순서

1. 현재위치 = 매장
2. 미방문 = 모든 배송지
3. 경로 = [매장]

4. WHILE 미방문이 비어있지 않음:
     최소거리 = ∞
     다음목적지 = NULL

     FOR EACH 배송지 IN 미방문:
       거리 = Haversine(현재위치, 배송지)
       IF 거리 < 최소거리:
         최소거리 = 거리
         다음목적지 = 배송지
       END IF
     END FOR

     경로.추가(다음목적지)
     미방문.제거(다음목적지)
     현재위치 = 다음목적지
   END WHILE

5. 경로.추가(매장) // 복귀
6. RETURN 경로
```

---

### 7.3 선형 보간 (Linear Interpolation)

드론이 두 지점 사이를 이동할 때 중간 위치를 계산합니다.

```java
// GeoUtils.interpolate()
public static double[] interpolate(
    double lat1, double lng1,
    double lat2, double lng2,
    double fraction // 0.0 ~ 1.0
) {
    double lat = lat1 + (lat2 - lat1) * fraction;
    double lng = lng1 + (lng2 - lng1) * fraction;
    return new double[]{lat, lng};
}
```

**예시**:
- 출발지: (37.0, 127.0)
- 목적지: (37.1, 127.1)
- fraction = 0.5 → 중간 지점: (37.05, 127.05)

---

### 7.4 WebSocket 실시간 통신

드론 위치를 실시간으로 브로드캐스트합니다.

**서버 설정**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
}
```

**브로드캐스트**
```java
messagingTemplate.convertAndSend(
    "/topic/route/" + routeId,
    positionData
);
```

**클라이언트 구독**
```javascript
stompClient.subscribe('/topic/route/1', (message) => {
    const data = JSON.parse(message.body);
    // 지도 업데이트
});
```

---

## 8. 성능 최적화 기법

### 8.1 N+1 문제 해결

**문제**: RouteStops를 조회할 때 각 Stop마다 추가 쿼리 발생

**해결책**: JOIN FETCH 사용
```java
@Query("SELECT r FROM Route r " +
       "LEFT JOIN FETCH r.routeStops " +
       "WHERE r.routeId = :routeId")
Optional<Route> findByIdWithDetails(@Param("routeId") Long routeId);
```

---

### 8.2 인덱스 활용

**거리 기반 검색 최적화**
```sql
CREATE INDEX idx_store_location ON store(lat, lng);
CREATE INDEX idx_store_active ON store(is_active);
```

---

### 8.3 비동기 처리

드론 시뮬레이션은 별도 스레드에서 실행:
```java
@Async
public void simulateFlight(Long routeId) {
    // 시뮬레이션 로직
}
```

---

## 9. API 사용 예시 요약

### 고객 앱 플로우
```
1. GET /api/users/me (내 정보 조회 - 사용자 위치 및 배송지 정보)
2. GET /api/stores?lat=37.4979&lng=127.0276 (배달 가능한 매장 조회)
3. GET /api/stores/1/delivery-info?lat=37.4979&lng=127.0276 (배송 정보 조회 - 무게 제한, 배송 가능 여부)
4. GET /api/stores/1/categories (선택한 매장의 카테고리 조회)
5. GET /api/stores/1/products?category=음료 (카테고리별 상품 조회)
6. POST /api/orders (주문 생성 - 서버에서 무게/거리 검증)
7. GET /api/orders/1 (폴링으로 상태 확인)
```

### 점주 앱 플로우
```
1. GET /api/stores/1/orders?status=CREATED (대기 중인 주문 목록 조회)
2. POST /api/routes/start-delivery { "orderIds": [1,2,3] } (선택한 주문 배송 시작)
3. GET /api/routes/active (진행 중인 배송 조회)
4. WebSocket 구독: /topic/route/{routeId} (실시간 위치)
5. GET /api/routes/1/current-position (현재 위치 조회)
6. GET /api/routes/1 (경로 상세 조회)
```

### 관리자 배치 처리 플로우
```
1. POST /api/routes/batch-delivery (모든 대기 주문 자동 배송 시작)
2. GET /api/routes/active (진행 중인 배송 조회)
```

---

## 10. 참고사항

- 모든 날짜/시간은 ISO 8601 형식 (`yyyy-MM-dd'T'HH:mm:ss`)
- 모든 거리는 킬로미터(km) 단위
- 모든 무게는 킬로그램(kg) 단위
- 모든 금액은 원(KRW) 단위
- BigDecimal 타입은 소수점 정밀도 유지
- nullable로 표시된 필드는 null 값 가능

**드론 시뮬레이터 파라미터**
- 업데이트 주기: 2초
- 드론 속도: 30 km/h (8.33 m/s)
- 초기 배터리: 100%
- 배터리 소모율: 거리당 5% (간단한 시뮬레이션)
- 비행 고도: 50m (고정)

---

**문서 버전**: 9.3
**최종 수정일**: 2025-11-28
**작성자**: Backend Development Team

**변경 이력**
- v9.3 (2025-11-28): 가게별 주문 조회 API 경로 리팩토링 ⭐⭐
  - **엔드포인트 경로 변경**: `/orders/store/{storeId}` → `/stores/{storeId}/orders` (더 RESTful한 설계)
  - **컨트롤러 이동**: OrderController → StoreController로 메서드 이동
  - **목차 업데이트**: "가게별 주문 목록 조회"를 매장 API 섹션(3.6)으로 이동
  - **사용 예시 업데이트**: 점주 앱 플로우 및 데모 시나리오의 경로 수정
  - **리소스 계층 구조 개선**: 매장 하위 리소스로 주문을 표현하여 API 구조 명확화
- v9.2 (2025-11-28): 사용자 API 섹션 추가 ⭐
  - **신규 섹션 추가**: "1. 사용자 (User) API" - 기존 섹션 번호 재정렬
  - **신규 API 문서화**: `GET /api/users/me` - 내 정보 조회 (이름, 주소, 위치)
  - **사용 예시 추가**: 고객 앱 플로우에 사용자 정보 조회 단계 추가
  - **목차 업데이트**: 사용자 API가 최상단에 위치하도록 재정렬
- v9.1 (2025-11-27): 가게별 주문 목록 조회 API 추가 ⭐
  - **신규 API 추가**: `GET /api/orders/store/{storeId}` - 특정 가게의 주문 목록 조회
  - **상태 필터링 지원**: `status` 쿼리 파라미터로 주문 상태 필터링 가능 (CREATED, ASSIGNED, FULFILLED, CANCELED, FAILED)
  - **점주 앱 플로우 개선**: 점주가 자신의 가게에 들어온 주문들을 조회하고 배송할 주문 선택 가능
  - **사용 예시 추가**: 배송 대기 중인 주문만 조회하는 케이스 추가
- v9.0 (2025-11-26): 배송 시작 API 변경 - 점주 선택 방식으로 전환 ⭐⭐⭐
  - **배송 시작 API 변경**: 점주가 주문 ID를 선택하여 배송 시작 (최대 3개)
  - **Request DTO 추가**: `StartDeliveryRequest` - orderIds 리스트 (최소 1개, 최대 3개)
  - **예외 클래스 추가**: `PayloadExceededException`, `BatteryInsufficientException`
  - **검증 로직 강화**:
    - 모든 주문이 CREATED 상태인지 확인
    - 모든 주문이 같은 매장인지 확인
    - 총 무게가 드론 최대 적재량을 초과하는지 검증
    - 예상 거리가 배터리 용량(16km)을 초과하는지 검증
  - **배송 배치 처리 API 신규 추가**: `POST /api/routes/batch-delivery` - 기존 자동 배치 기능
  - **우선순위 변경**: 주문 시간순 (createdAt) 기준으로 정렬 후 경로 최적화
  - **배터리 제한**: 최대 16km (배터리 20% 여유 확보, km당 5% 소모)
- v8.0 (2025-11-25): 드론-매장 소속 관계 추가 ⭐
  - **Drone-Store 관계 추가**: Drone 엔티티에 store_id FK 추가 (Drone N:1 Store)
  - **배치 처리 로직 개선**: 각 매장의 주문을 해당 매장 소속 드론만 처리하도록 변경
  - **데모 데이터 확장**: 드론 10대 → 40대로 증가 (각 매장당 1대씩 배치)
  - **DroneRepository 메서드 추가**: `findFirstByStoreAndStatus()` - 매장별 드론 조회
  - **현실성 향상**: 드론이 자신의 소속 매장 주문만 배송하도록 개선
- v7.0 (2025-11-25): 데이터베이스 스키마 리팩토링 - Customer/User 통합 ⭐⭐
  - **Customer 테이블 삭제**: User 테이블로 통합 (role 필드 추가: CUSTOMER, OWNER)
  - **Store-Owner 관계 추가**: Store 엔티티에 owner_id FK 추가 (Store ↔ User(OWNER) 관계)
  - **API 변경**: `customerId` → `userId` 파라미터명 변경
  - **에러 코드 변경**: `CUSTOMER_NOT_FOUND` → `USER_NOT_FOUND`
  - **용어 통일**: "고객" → "사용자"로 전체 용어 통일
- v6.0 (2025-11-22): 치명적 버그 수정 및 용어 통일, 응답 최적화 ⭐⭐⭐
  - **치명적 버그 수정**: 주문 완료 로직 추가 (배송 완료 시 주문 상태가 FULFILLED로 변경됨)
  - **WebSocket 배송 완료 알림 추가**: `/topic/order/{orderId}` 토픽으로 실시간 완료 알림 전송
  - **용어 통일**: `requestId` → `orderId`, `requestItemId` → `orderItemId`
  - **DB 테이블 이름 변경**: `delivery_request` → `orders`, `request_item` → `order_item`, `route_stop_request` → `route_stop_order`
  - **응답 데이터 최적화**: OrderResponse에서 불필요한 필드 제거 (customerName, customerAddress, totalWeightKg, itemCount, unitWeightKg)
  - **DB 스키마 개선**: `canceled_at`, `failure_reason` 필드 추가
  - **매장 조회 로직 개선**:
    - 각 매장의 배달 가능 거리(`deliveryRadiusKm`) 내에 사용자가 있는 매장만 반환
    - `radius` 파라미터 제거 (각 매장의 배달 가능 거리만 체크)
- v5.0 (2025-11-21): 주문 검증 로직 추가 및 배송 정보 API 신규 추가
  - **배송 정보 조회 API 추가**: `GET /api/stores/{storeId}/delivery-info`
  - 주문 생성 시 **드론 최대 적재 무게 검증** 추가 (초과 시 `ORDER_TOTAL_WEIGHT_EXCEEDED`)
  - 주문 생성 시 **배송 가능 거리 검증** 추가 (초과 시 `STORE_OUT_OF_DELIVERY_RANGE`)
  - 프론트엔드에서 실시간 무게 제한 및 배송 가능 여부 검증 가능
- v4.0 (2025-11-21): API 구조 개선
  - 전체 매장 조회 API 제거 → 주변 매장 조회로 통합 (기본 반경 10km)
  - Product API 전체 제거 (상품 조회는 매장 선택 후 진행)
  - 매장 상품 조회 API: 카테고리 파라미터 선택적 제공으로 전체/카테고리별 조회 지원
- v3.0 (2025-11-21): RESTful 응답 형식 적용, 주문 생성 응답 간소화 (orderId만 반환)
- v2.0 (2024-01-15): 추가 API 구현 (매장 검색, 전체 상품 조회 등)
- v1.0 (2024-01-10): 초기 버전
