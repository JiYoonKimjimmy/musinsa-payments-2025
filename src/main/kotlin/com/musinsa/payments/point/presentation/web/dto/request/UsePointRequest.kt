package com.musinsa.payments.point.presentation.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 포인트 사용 요청 DTO
 */
@Schema(description = "포인트 사용 요청")
data class UsePointRequest(
    @field:NotNull(message = "회원 ID는 필수입니다.")
    @field:Positive(message = "회원 ID는 0보다 커야 합니다.")
    @field:Schema(description = "회원 ID", example = "12345", required = true)
    val memberId: Long,
    
    @field:NotBlank(message = "주문번호는 필수입니다.")
    @field:Schema(description = "주문 번호", example = "ORDER-2025-01-01-001", required = true)
    val orderNumber: String,
    
    @field:NotNull(message = "사용 금액은 필수입니다.")
    @field:Min(value = 1, message = "사용 금액은 1원 이상이어야 합니다.")
    @field:Schema(description = "사용 금액", example = "10000", required = true)
    val amount: Long
)

