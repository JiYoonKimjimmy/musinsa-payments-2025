package com.musinsa.payments.point.domain.entity

/**
 * 포인트 사용 상태
 */
enum class PointUsageStatus {
    USED,                  // 사용됨
    PARTIALLY_CANCELLED,   // 부분 취소됨
    FULLY_CANCELLED        // 전체 취소됨
}
