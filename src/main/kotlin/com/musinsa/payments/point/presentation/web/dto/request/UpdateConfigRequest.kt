package com.musinsa.payments.point.presentation.web.dto.request

import jakarta.validation.constraints.NotBlank

/**
 * 설정 업데이트 요청 DTO
 */
data class UpdateConfigRequest(
    @field:NotBlank(message = "설정 값은 필수입니다.")
    val configValue: String,
    
    val description: String? = null
)

