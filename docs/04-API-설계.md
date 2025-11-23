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
```kotlin
data class AccumulatePointRequest(
    @field:NotNull
    val memberId: Long,
    
    @field:NotNull
    @field:Min(1)
    val amount: Long,
    
    @field:Min(1)
    @field:Max(1824)  // 약 5년
    val expirationDays: Int? = null,  // 옵션, 기본값: 설정값 사용
    
    val isManualGrant: Boolean = false  // 기본값: false
)
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
```kotlin
data class AccumulatePointResponse(
    val pointKey: String,
    val memberId: Long,
    val amount: Long,
    val availableAmount: Long,
    val expirationDate: LocalDate,
    val isManualGrant: Boolean,
    val status: String,
    val createdAt: LocalDateTime
)
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
```kotlin
data class CancelAccumulationRequest(
    val reason: String? = null  // 옵션
)
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
```kotlin
data class CancelAccumulationResponse(
    val pointKey: String,
    val targetPointKey: String,
    val cancellationType: String,
    val memberId: Long,
    val amount: Long,
    val reason: String?,
    val createdAt: LocalDateTime
)
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
```kotlin
data class UsePointRequest(
    @field:NotNull
    val memberId: Long,
    
    @field:NotBlank
    val orderNumber: String,
    
    @field:NotNull
    @field:Min(1)
    val amount: Long
)
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
```kotlin
data class UsePointResponse(
    val pointKey: String,
    val memberId: Long,
    val orderNumber: String,
    val totalAmount: Long,
    val cancelledAmount: Long,
    val status: String,
    val usageDetails: List<UsageDetailResponse>,
    val createdAt: LocalDateTime
)

data class UsageDetailResponse(
    val pointAccumulationKey: String,
    val amount: Long
)
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
```kotlin
data class CancelUsageRequest(
    @field:Min(1)
    val amount: Long? = null,  // 취소할 금액 (전체 취소 시 null 가능, null이 아닐 경우 1 이상)
    
    val reason: String? = null  // 옵션
)
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
```kotlin
data class CancelUsageResponse(
    val pointKey: String,
    val targetPointKey: String,
    val cancellationType: String,
    val memberId: Long,
    val amount: Long,
    val reason: String?,
    val newAccumulations: List<NewAccumulationResponse>,  // 만료 포인트로 인한 신규 적립
    val createdAt: LocalDateTime
)

data class NewAccumulationResponse(
    val pointKey: String,
    val amount: Long,
    val reason: String?
)
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
```kotlin
data class PointBalanceResponse(
    val memberId: Long,
    val totalBalance: Long,
    val availableBalance: Long,
    val expiredBalance: Long,
    val accumulations: List<AccumulationSummaryResponse>
)

data class AccumulationSummaryResponse(
    val pointKey: String,
    val amount: Long,
    val availableAmount: Long,
    val expirationDate: LocalDate,
    val isManualGrant: Boolean,
    val status: String
)
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
```kotlin
data class PointHistoryResponse(
    val content: List<UsageHistoryResponse>,
    val page: PageInfo
)

data class UsageHistoryResponse(
    val pointKey: String,
    val orderNumber: String,
    val totalAmount: Long,
    val cancelledAmount: Long,
    val status: String,
    val usageDetails: List<UsageDetailResponse>,
    val createdAt: LocalDateTime
)

data class PageInfo(
    val number: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
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

---

**다음 문서**: [05. 비즈니스 로직 설계](./05-비즈니스-로직-설계.md)

