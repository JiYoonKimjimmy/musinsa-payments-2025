package com.musinsa.payments.point.presentation.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

/**
 * 포인트 사용 취소 요청 DTO
 */
@Schema(description = "포인트 사용 취소 요청")
data class CancelUsageRequest(
    @field:Min(value = 1, message = "취소 금액은 1원 이상이어야 합니다.")
    @field:Schema(description = "취소 금액 (전체 취소 시 생략 가능)", example = "5000", required = false)
    val amount: Long? = null,
    
    @field:Schema(description = "취소 사유", example = "주문 취소", required = false)
    val reason: String? = null
)

