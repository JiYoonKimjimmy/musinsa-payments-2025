package com.musinsa.payments.point.domain.valueobject

/**
 * 주문번호 값 객체
 * 주문을 식별하는 고유 번호입니다.
 */
data class OrderNumber(
    val value: String
) {
    companion object {
        fun of(value: String): OrderNumber {
            require(value.isNotBlank()) { "주문번호는 필수입니다." }
            return OrderNumber(value)
        }
    }
}
