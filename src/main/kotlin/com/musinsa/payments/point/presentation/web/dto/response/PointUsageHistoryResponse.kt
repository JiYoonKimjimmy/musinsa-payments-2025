package com.musinsa.payments.point.presentation.web.dto.response

import java.time.LocalDateTime

/**
 * 포인트 사용 내역 응답 DTO
 */
data class PointUsageHistoryResponse(
    val content: List<PointUsageHistoryItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * 포인트 사용 내역 항목
 */
data class PointUsageHistoryItem(
    val pointKey: String,
    val memberId: Long,
    val orderNumber: String,
    val totalAmount: Long,
    val cancelledAmount: Long,
    val remainingAmount: Long,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

