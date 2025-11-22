package com.musinsa.payments.point.domain.exception

/**
 * 최대 적립 금액 초과 예외
 * 1회 최대 적립 금액을 초과할 때 발생합니다.
 */
class MaxAccumulationExceededException(message: String) : PointDomainException(message)
