package com.musinsa.payments.point.presentation.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 포인트 적립 취소 응답 DTO
 */
@Schema(description = "포인트 적립 취소 응답")
data class CancelAccumulationResponse(
    @field:Schema(description = "포인트 키", example = "point-key-12345")
    val pointKey: String,
    @field:Schema(description = "회원 ID", example = "12345")
    val memberId: Long,
    @field:Schema(description = "적립 금액", example = "10000")
    val amount: Long,
    @field:Schema(description = "사용 가능 금액", example = "0")
    val availableAmount: Long,
    @field:Schema(description = "만료일")
    val expirationDate: LocalDate,
    @field:Schema(description = "수동 지급 여부", example = "false")
    val isManualGrant: Boolean,
    @field:Schema(description = "상태", example = "CANCELLED")
    val status: String,
    @field:Schema(description = "생성 일시")
    val createdAt: LocalDateTime,
    @field:Schema(description = "수정 일시")
    val updatedAt: LocalDateTime
)

