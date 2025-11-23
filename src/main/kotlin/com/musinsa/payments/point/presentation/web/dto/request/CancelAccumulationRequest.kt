package com.musinsa.payments.point.presentation.web.dto.request

/**
 * 포인트 적립 취소 요청 DTO
 */
data class CancelAccumulationRequest(
    val reason: String? = null
)

