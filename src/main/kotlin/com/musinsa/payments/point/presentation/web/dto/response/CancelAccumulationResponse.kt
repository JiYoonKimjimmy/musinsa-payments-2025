package com.musinsa.payments.point.presentation.web.dto.response

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 포인트 적립 취소 응답 DTO
 */
data class CancelAccumulationResponse(
    val pointKey: String,
    val memberId: Long,
    val amount: Long,
    val availableAmount: Long,
    val expirationDate: LocalDate,
    val isManualGrant: Boolean,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

