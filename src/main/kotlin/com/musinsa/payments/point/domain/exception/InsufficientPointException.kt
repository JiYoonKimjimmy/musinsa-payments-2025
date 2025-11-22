package com.musinsa.payments.point.domain.exception

/**
 * 잔액 부족 예외
 * 사용 가능한 포인트가 부족할 때 발생합니다.
 */
class InsufficientPointException : PointDomainException(
    "사용 가능한 포인트가 부족합니다."
)
