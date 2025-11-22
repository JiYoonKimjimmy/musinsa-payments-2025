package com.musinsa.payments.point.domain.exception

/**
 * 최대 보유 금액 초과 예외
 * 개인별 최대 보유 금액을 초과할 때 발생합니다.
 */
class MaxBalanceExceededException : PointDomainException(
    "개인별 최대 보유 금액을 초과했습니다."
)
