package com.musinsa.payments.point.domain.exception

/**
 * 사용 취소 불가 예외
 * 취소할 수 없는 사용 건일 때 발생합니다.
 */
class CannotCancelUsageException : PointDomainException(
    "취소할 수 없는 사용 건입니다."
)
