package com.musinsa.payments.point.domain.entity

/**
 * 포인트 적립 상태
 */
enum class PointAccumulationStatus {
    ACCUMULATED,  // 적립됨
    CANCELLED,    // 취소됨
    EXPIRED       // 만료됨
}
