package com.musinsa.payments.point.presentation.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 포인트 사용 취소 응답 DTO
 */
@Schema(description = "포인트 사용 취소 응답")
data class CancelUsageResponse(
    @field:Schema(description = "포인트 키", example = "point-key-12345")
    val pointKey: String,
    @field:Schema(description = "회원 ID", example = "12345")
    val memberId: Long,
    @field:Schema(description = "주문 번호", example = "ORDER-2025-01-01-001")
    val orderNumber: String,
    @field:Schema(description = "사용 금액", example = "10000")
    val totalAmount: Long,
    @field:Schema(description = "취소된 금액", example = "10000")
    val cancelledAmount: Long,
    @field:Schema(description = "상태", example = "CANCELLED")
    val status: String,
    @field:Schema(description = "생성 일시")
    val createdAt: LocalDateTime,
    @field:Schema(description = "수정 일시")
    val updatedAt: LocalDateTime
)

