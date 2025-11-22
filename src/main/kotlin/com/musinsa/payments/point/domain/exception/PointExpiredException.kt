package com.musinsa.payments.point.domain.exception

/**
 * 만료된 포인트 예외
 * 만료된 포인트에 대한 작업 시 발생합니다.
 */
class PointExpiredException : PointDomainException(
    "만료된 포인트입니다."
)
