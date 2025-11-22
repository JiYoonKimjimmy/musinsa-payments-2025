package com.musinsa.payments.point.domain.exception

/**
 * 적립 취소 불가 예외
 * 이미 사용된 포인트는 적립 취소할 수 없을 때 발생합니다.
 */
class CannotCancelAccumulationException : PointDomainException(
    "이미 사용된 포인트는 적립 취소할 수 없습니다."
)
