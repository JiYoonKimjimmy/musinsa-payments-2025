package com.musinsa.payments.point.domain.exception

/**
 * 설정을 찾을 수 없을 때 발생하는 예외
 */
class ConfigNotFoundException(message: String) : PointDomainException(message)

