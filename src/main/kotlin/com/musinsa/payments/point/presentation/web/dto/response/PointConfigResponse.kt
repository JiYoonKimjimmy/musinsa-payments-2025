package com.musinsa.payments.point.presentation.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 포인트 설정 응답 DTO
 */
@Schema(description = "포인트 설정 응답")
data class PointConfigResponse(
    @field:Schema(description = "설정 키", example = "MAX_ACCUMULATION_AMOUNT_PER_TIME")
    val configKey: String,
    @field:Schema(description = "설정 값", example = "100000")
    val configValue: String,
    @field:Schema(description = "설정 설명", example = "1회 최대 적립 금액")
    val description: String?,
    @field:Schema(description = "수정 일시")
    val updatedAt: LocalDateTime
)

