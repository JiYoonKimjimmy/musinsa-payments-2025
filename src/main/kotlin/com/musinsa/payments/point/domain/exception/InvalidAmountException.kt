package com.musinsa.payments.point.domain.exception

/**
 * 잘못된 금액 예외
 * 금액 검증 실패 시 발생합니다.
 */
class InvalidAmountException(message: String) : PointDomainException(message)
