package com.musinsa.payments.point.presentation.web.dto.response

import java.time.LocalDateTime

/**
 * 포인트 사용 취소 응답 DTO
 */
data class CancelUsageResponse(
    val pointKey: String,
    val memberId: Long,
    val orderNumber: String,
    val totalAmount: Long,
    val cancelledAmount: Long,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

