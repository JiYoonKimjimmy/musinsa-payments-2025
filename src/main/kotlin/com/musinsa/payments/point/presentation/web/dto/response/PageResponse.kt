package com.musinsa.payments.point.presentation.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 페이징 응답 DTO
 */
@Schema(description = "페이징 응답")
data class PageResponse<T>(
    @field:Schema(description = "데이터 목록")
    val content: List<T>,
    @field:Schema(description = "현재 페이지 번호", example = "0")
    val page: Int,
    @field:Schema(description = "페이지 크기", example = "20")
    val size: Int,
    @field:Schema(description = "전체 요소 개수", example = "100")
    val totalElements: Long,
    @field:Schema(description = "전체 페이지 수", example = "5")
    val totalPages: Int,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
    @field:Schema(description = "이전 페이지 존재 여부", example = "false")
    val hasPrevious: Boolean
)

