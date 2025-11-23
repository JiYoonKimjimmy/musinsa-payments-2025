package com.musinsa.payments.point.presentation.web.dto.request

import jakarta.validation.constraints.Min

/**
 * 포인트 사용 취소 요청 DTO
 */
data class CancelUsageRequest(
    @field:Min(value = 1, message = "취소 금액은 1원 이상이어야 합니다.")
    val amount: Long? = null,
    
    val reason: String? = null
)

