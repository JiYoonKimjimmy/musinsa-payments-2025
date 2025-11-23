package com.musinsa.payments.point.presentation.web.dto.response

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 포인트 적립 응답 DTO
 */
data class AccumulatePointResponse(
    val pointKey: String,
    val memberId: Long,
    val amount: Long,
    val availableAmount: Long,
    val expirationDate: LocalDate,
    val isManualGrant: Boolean,
    val status: String,
    val createdAt: LocalDateTime
)

