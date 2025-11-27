package com.musinsa.payments.point.presentation.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * 페이징 요청 DTO
 */
@Schema(description = "페이징 요청")
data class PageRequest(
    @field:Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    @field:Schema(description = "페이지 번호 (0부터 시작)", example = "0", required = false)
    val page: Int = 0,
    
    @field:Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @field:Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    @field:Schema(description = "페이지 크기", example = "20", required = false)
    val size: Int = 20
)

