package com.musinsa.payments.point.presentation.web.dto.response

import java.time.LocalDateTime

/**
 * 포인트 설정 응답 DTO
 */
data class PointConfigResponse(
    val configKey: String,
    val configValue: String,
    val description: String?,
    val updatedAt: LocalDateTime
)

