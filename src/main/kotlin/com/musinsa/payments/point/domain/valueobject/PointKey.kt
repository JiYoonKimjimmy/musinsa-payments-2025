package com.musinsa.payments.point.domain.valueobject

import java.util.*

/**
 * 포인트 키 값 객체
 * 포인트 적립/사용/취소 건을 식별하는 고유 키입니다.
 */
data class PointKey(
    val value: String
) {
    companion object {
        fun of(value: String): PointKey {
            require(value.isNotBlank()) { "포인트 키는 필수입니다." }
            return PointKey(value)
        }
        
        fun generate(): PointKey {
            // UUID 기반 생성 (8자리 대문자)
            return PointKey(
                UUID.randomUUID().toString().substring(0, 8).uppercase()
            )
        }
    }
}
