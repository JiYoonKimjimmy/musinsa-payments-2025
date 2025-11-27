package com.musinsa.payments.point.presentation.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * 설정 업데이트 요청 DTO
 */
@Schema(description = "설정 업데이트 요청")
data class UpdateConfigRequest(
    @field:NotBlank(message = "설정 값은 필수입니다.")
    @field:Schema(description = "설정 값", example = "100000", required = true)
    val configValue: String,
    
    @field:Schema(description = "설정 설명", example = "1회 최대 적립 금액", required = false)
    val description: String? = null
)

