package com.musinsa.payments.point.presentation.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 포인트 적립 취소 요청 DTO
 */
@Schema(description = "포인트 적립 취소 요청")
data class CancelAccumulationRequest(
    @field:Schema(description = "취소 사유", example = "고객 요청", required = false)
    val reason: String? = null
)

