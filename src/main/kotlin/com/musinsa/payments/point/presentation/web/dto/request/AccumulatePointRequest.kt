package com.musinsa.payments.point.presentation.web.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 포인트 적립 요청 DTO
 */
data class AccumulatePointRequest(
    @field:NotNull(message = "회원 ID는 필수입니다.")
    @field:Positive(message = "회원 ID는 0보다 커야 합니다.")
    val memberId: Long,
    
    @field:NotNull(message = "적립 금액은 필수입니다.")
    @field:Min(value = 1, message = "적립 금액은 1원 이상이어야 합니다.")
    val amount: Long,
    
    @field:Min(value = 1, message = "만료일은 1일 이상이어야 합니다.")
    @field:Max(value = 1824, message = "만료일은 1824일(약 5년) 이하여야 합니다.")
    val expirationDays: Int? = null,
    
    val isManualGrant: Boolean = false
)

