package com.musinsa.payments.point.domain.exception

/**
 * 설정 값이 유효하지 않을 때 발생하는 예외
 */
class InvalidConfigValueException(message: String) : PointDomainException(message)

