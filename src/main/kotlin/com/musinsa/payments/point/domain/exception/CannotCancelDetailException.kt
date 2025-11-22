package com.musinsa.payments.point.domain.exception

/**
 * 상세 내역 취소 불가 예외
 * 상세 내역을 취소할 수 없을 때 발생합니다.
 */
class CannotCancelDetailException : PointDomainException(
    "상세 내역을 취소할 수 없습니다."
)
