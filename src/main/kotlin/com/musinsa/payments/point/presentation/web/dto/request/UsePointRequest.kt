package com.musinsa.payments.point.presentation.web.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 포인트 사용 요청 DTO
 */
data class UsePointRequest(
    @field:NotNull(message = "회원 ID는 필수입니다.")
    @field:Positive(message = "회원 ID는 0보다 커야 합니다.")
    val memberId: Long,
    
    @field:NotBlank(message = "주문번호는 필수입니다.")
    val orderNumber: String,
    
    @field:NotNull(message = "사용 금액은 필수입니다.")
    @field:Min(value = 1, message = "사용 금액은 1원 이상이어야 합니다.")
    val amount: Long
)

