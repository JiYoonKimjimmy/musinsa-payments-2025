package com.musinsa.payments.point.presentation.web.dto.response

/**
 * 페이징 응답 DTO
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

