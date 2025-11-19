# 04. API 설계

## 1. 개요

무료 포인트 시스템의 REST API를 설계합니다. 적립, 적립 취소, 사용, 사용 취소, 조회 기능을 제공합니다.

## 2. API 기본 규칙

### 2.1 기본 URL
```
/api/points
```

### 2.2 HTTP 메서드
- `GET`: 조회
- `POST`: 생성/처리

### 2.3 응답 형식
- 성공: HTTP 200 OK
- 실패: HTTP 4xx (클라이언트 오류), 5xx (서버 오류)
- 응답 본문: JSON 형식

### 2.4 공통 응답 구조

#### 성공 응답
```json
{
  "success": true,
  "data": { ... },
  "message": "성공 메시지"
}
```

#### 실패 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

## 3. API 엔드포인트

### 3.1 포인트 적립

#### POST /api/points/accumulate

**설명**: 포인트를 적립합니다.

**Request Body**:
```json
{
  "memberId": 1,
  "amount": 1000,
  "expirationDays": 365,
  "isManualGrant": false
}
```

**Request DTO**:
```java
public class AccumulatePointRequest {
    @NotNull
    private Long memberId;
    
    @NotNull
    @Min(1)
    private Long amount;
    
    @Min(1)
    @Max(1824)  // 약 5년
    private Integer expirationDays;  // 옵션, 기본값: 설정값 사용
    
    private Boolean isManualGrant = false;  // 기본값: false
}
```

**Response Body**:
```json
{
  "success": true,
  "data": {
    "pointKey": "A",
    "memberId": 1,
    "amount": 1000,
    "availableAmount": 1000,
    "expirationDate": "2025-12-31",
    "isManualGrant": false,
    "status": "ACCUMULATED",
    "createdAt": "2025-01-01T10:00:00"
  }
}
```

**Response DTO**:
```java
public class AccumulatePointResponse {
    private String pointKey;
    private Long memberId;
    private Long amount;
    private Long availableAmount;
    private LocalDate expirationDate;
    private Boolean isManualGrant;
    private String status;
    private LocalDateTime createdAt;
}
```

**에러 케이스**:
- `400 Bad Request`: 요청 데이터 검증 실패
- `400 INVALID_AMOUNT`: 적립 금액이 범위를 벗어남
- `400 EXCEEDED_MAX_BALANCE`: 개인별 최대 보유 금액 초과
- `404 MEMBER_NOT_FOUND`: 사용자를 찾을 수 없음

### 3.2 포인트 적립 취소

#### POST /api/points/accumulate/{pointKey}/cancel

**설명**: 특정 적립 건을 취소합니다.

**Path Parameter**:
- `pointKey`: 취소할 적립 건의 포인트 키

**Request Body**:
```json
{
  "reason": "적립 오류"
}
```

**Request DTO**:
```java
public class CancelAccumulationRequest {
    private String reason;  // 옵션
}
```

**Response Body**:
```json
{
  "success": true,
  "data": {
    "pointKey": "CANCEL_001",
    "targetPointKey": "A",
    "cancellationType": "ACCUMULATION_CANCELLATION",
    "memberId": 1,
    "amount": 1000,
    "reason": "적립 오류",
    "createdAt": "2025-01-01T11:00:00"
  }
}
```

**Response DTO**:
```java
public class CancelAccumulationResponse {
    private String pointKey;
    private String targetPointKey;
    private String cancellationType;
    private Long memberId;
    private Long amount;
    private String reason;
    private LocalDateTime createdAt;
}
```

**에러 케이스**:
- `404 ACCUMULATION_NOT_FOUND`: 적립 건을 찾을 수 없음
- `400 CANNOT_CANCEL_ACCUMULATION`: 이미 사용된 포인트는 취소 불가
- `400 ALREADY_CANCELLED`: 이미 취소된 적립 건

### 3.3 포인트 사용

#### POST /api/points/use

**설명**: 주문 시 포인트를 사용합니다.

**Request Body**:
```json
{
  "memberId": 1,
  "orderNumber": "A1234",
  "amount": 1200
}
```

**Request DTO**:
```java
public class UsePointRequest {
    @NotNull
    private Long memberId;
    
    @NotBlank
    private String orderNumber;
    
    @NotNull
    @Min(1)
    private Long amount;
}
```

**Response Body**:
```json
{
  "success": true,
  "data": {
    "pointKey": "C",
    "memberId": 1,
    "orderNumber": "A1234",
    "totalAmount": 1200,
    "cancelledAmount": 0,
    "status": "USED",
    "usageDetails": [
      {
        "pointAccumulationKey": "A",
        "amount": 1000
      },
      {
        "pointAccumulationKey": "B",
        "amount": 200
      }
    ],
    "createdAt": "2025-01-01T12:00:00"
  }
}
```

**Response DTO**:
```java
public class UsePointResponse {
    private String pointKey;
    private Long memberId;
    private String orderNumber;
    private Long totalAmount;
    private Long cancelledAmount;
    private String status;
    private List<UsageDetailResponse> usageDetails;
    private LocalDateTime createdAt;
}

public class UsageDetailResponse {
    private String pointAccumulationKey;
    private Long amount;
}
```

**에러 케이스**:
- `400 INSUFFICIENT_POINT`: 사용 가능한 포인트 부족
- `404 MEMBER_NOT_FOUND`: 사용자를 찾을 수 없음

### 3.4 포인트 사용 취소

#### POST /api/points/use/{pointKey}/cancel

**설명**: 사용한 포인트를 취소합니다. 전체 또는 부분 취소 가능합니다.

**Path Parameter**:
- `pointKey`: 취소할 사용 건의 포인트 키

**Request Body**:
```json
{
  "amount": 1100,
  "reason": "주문 취소"
}
```

**Request DTO**:
```java
public class CancelUsageRequest {
    @NotNull
    @Min(1)
    private Long amount;  // 취소할 금액 (전체 취소 시 null 가능)
    
    private String reason;  // 옵션
}
```

**Response Body**:
```json
{
  "success": true,
  "data": {
    "pointKey": "D",
    "targetPointKey": "C",
    "cancellationType": "USAGE_CANCELLATION",
    "memberId": 1,
    "amount": 1100,
    "reason": "주문 취소",
    "newAccumulations": [
      {
        "pointKey": "E",
        "amount": 1000,
        "reason": "만료된 포인트 사용 취소로 인한 신규 적립"
      }
    ],
    "createdAt": "2025-01-01T13:00:00"
  }
}
```

**Response DTO**:
```java
public class CancelUsageResponse {
    private String pointKey;
    private String targetPointKey;
    private String cancellationType;
    private Long memberId;
    private Long amount;
    private String reason;
    private List<NewAccumulationResponse> newAccumulations;  // 만료 포인트로 인한 신규 적립
    private LocalDateTime createdAt;
}

public class NewAccumulationResponse {
    private String pointKey;
    private Long amount;
    private String reason;
}
```

**에러 케이스**:
- `404 USAGE_NOT_FOUND`: 사용 건을 찾을 수 없음
- `400 CANNOT_CANCEL_USAGE`: 취소할 수 없는 사용 건
- `400 EXCEEDED_CANCEL_AMOUNT`: 취소 금액이 사용 금액을 초과

### 3.5 포인트 잔액 조회

#### GET /api/points/balance/{memberId}

**설명**: 사용자의 포인트 잔액을 조회합니다.

**Path Parameter**:
- `memberId`: 사용자 ID

**Response Body**:
```json
{
  "success": true,
  "data": {
    "memberId": 1,
    "totalBalance": 1500,
    "availableBalance": 300,
    "expiredBalance": 0,
    "accumulations": [
      {
        "pointKey": "A",
        "amount": 1000,
        "availableAmount": 0,
        "expirationDate": "2025-12-31",
        "isManualGrant": false,
        "status": "ACCUMULATED"
      },
      {
        "pointKey": "B",
        "amount": 500,
        "availableAmount": 300,
        "expirationDate": "2025-12-31",
        "isManualGrant": false,
        "status": "ACCUMULATED"
      }
    ]
  }
}
```

**Response DTO**:
```java
public class PointBalanceResponse {
    private Long memberId;
    private Long totalBalance;
    private Long availableBalance;
    private Long expiredBalance;
    private List<AccumulationSummaryResponse> accumulations;
}

public class AccumulationSummaryResponse {
    private String pointKey;
    private Long amount;
    private Long availableAmount;
    private LocalDate expirationDate;
    private Boolean isManualGrant;
    private String status;
}
```

**에러 케이스**:
- `404 MEMBER_NOT_FOUND`: 사용자를 찾을 수 없음

### 3.6 포인트 사용 내역 조회

#### GET /api/points/history/{memberId}

**설명**: 사용자의 포인트 사용 내역을 조회합니다.

**Path Parameter**:
- `memberId`: 사용자 ID

**Query Parameters**:
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 20)
- `orderNumber`: 주문번호로 필터링 (옵션)

**Response Body**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "pointKey": "C",
        "orderNumber": "A1234",
        "totalAmount": 1200,
        "cancelledAmount": 0,
        "status": "USED",
        "usageDetails": [
          {
            "pointAccumulationKey": "A",
            "amount": 1000
          },
          {
            "pointAccumulationKey": "B",
            "amount": 200
          }
        ],
        "createdAt": "2025-01-01T12:00:00"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

**Response DTO**:
```java
public class PointHistoryResponse {
    private List<UsageHistoryResponse> content;
    private PageInfo page;
}

public class UsageHistoryResponse {
    private String pointKey;
    private String orderNumber;
    private Long totalAmount;
    private Long cancelledAmount;
    private String status;
    private List<UsageDetailResponse> usageDetails;
    private LocalDateTime createdAt;
}

public class PageInfo {
    private Integer number;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
}
```

**에러 케이스**:
- `404 MEMBER_NOT_FOUND`: 사용자를 찾을 수 없음

## 4. 에러 코드 정의

### 4.1 공통 에러 코드

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `INVALID_REQUEST` | 400 | 요청 데이터 검증 실패 |
| `MEMBER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 |
| `INTERNAL_SERVER_ERROR` | 500 | 내부 서버 오류 |

### 4.2 포인트 적립 에러 코드

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `INVALID_AMOUNT` | 400 | 적립 금액이 범위를 벗어남 |
| `EXCEEDED_MAX_ACCUMULATION` | 400 | 1회 최대 적립 금액 초과 |
| `EXCEEDED_MAX_BALANCE` | 400 | 개인별 최대 보유 금액 초과 |
| `INVALID_EXPIRATION_DATE` | 400 | 만료일이 범위를 벗어남 |

### 4.3 포인트 적립 취소 에러 코드

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `ACCUMULATION_NOT_FOUND` | 404 | 적립 건을 찾을 수 없음 |
| `CANNOT_CANCEL_ACCUMULATION` | 400 | 이미 사용된 포인트는 취소 불가 |
| `ALREADY_CANCELLED` | 400 | 이미 취소된 적립 건 |

### 4.4 포인트 사용 에러 코드

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `INSUFFICIENT_POINT` | 400 | 사용 가능한 포인트 부족 |
| `INVALID_ORDER_NUMBER` | 400 | 주문번호가 유효하지 않음 |

### 4.5 포인트 사용 취소 에러 코드

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `USAGE_NOT_FOUND` | 404 | 사용 건을 찾을 수 없음 |
| `CANNOT_CANCEL_USAGE` | 400 | 취소할 수 없는 사용 건 |
| `EXCEEDED_CANCEL_AMOUNT` | 400 | 취소 금액이 사용 금액을 초과 |

## 5. API 문서화

### 5.1 Swagger/OpenAPI 설정

- SpringDoc OpenAPI 3 사용
- `/swagger-ui.html`에서 API 문서 확인 가능
- `/v3/api-docs`에서 OpenAPI 스펙 확인 가능

### 5.2 API 문서 포함 내용

- 각 엔드포인트의 설명
- Request/Response 예시
- 에러 코드 및 설명
- 인증/인가 정보 (필요 시)

## 6. 다음 단계

다음 단계에서는 비즈니스 로직 설계를 통해 서비스 레이어의 구체적인 구현 방법을 정의할 예정입니다.


