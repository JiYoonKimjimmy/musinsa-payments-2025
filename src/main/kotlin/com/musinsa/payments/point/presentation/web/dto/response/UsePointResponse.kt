package com.musinsa.payments.point.presentation.web.dto.response

import java.time.LocalDateTime

/**
 * 포인트 사용 응답 DTO
 */
data class UsePointResponse(
    val pointKey: String,
    val memberId: Long,
    val orderNumber: String,
    val totalAmount: Long,
    val cancelledAmount: Long,
    val status: String,
    val createdAt: LocalDateTime
)

