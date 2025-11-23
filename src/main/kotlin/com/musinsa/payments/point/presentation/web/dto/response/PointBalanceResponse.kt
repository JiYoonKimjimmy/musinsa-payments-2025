package com.musinsa.payments.point.presentation.web.dto.response

import java.time.LocalDate

/**
 * 포인트 잔액 응답 DTO
 */
data class PointBalanceResponse(
    val memberId: Long,
    val totalBalance: Long,
    val availableBalance: Long,
    val expiredBalance: Long,
    val accumulations: List<PointAccumulationItem>
)

/**
 * 포인트 적립 내역 항목
 */
data class PointAccumulationItem(
    val pointKey: String,
    val amount: Long,
    val availableAmount: Long,
    val expirationDate: LocalDate,
    val isManualGrant: Boolean,
    val status: String
)

