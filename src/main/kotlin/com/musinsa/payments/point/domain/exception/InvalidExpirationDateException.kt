package com.musinsa.payments.point.domain.exception

/**
 * 잘못된 만료일 예외
 * 만료일 검증 실패 시 발생합니다.
 */
class InvalidExpirationDateException(message: String) : PointDomainException(message)
