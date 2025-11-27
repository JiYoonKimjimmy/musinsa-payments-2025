package com.musinsa.payments.point.presentation.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 포인트 사용 내역 응답 DTO
 */
@Schema(description = "포인트 사용 내역 응답")
data class PointUsageHistoryResponse(
    @field:Schema(description = "사용 내역 목록")
    val content: List<PointUsageHistoryItem>,
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

/**
 * 포인트 사용 내역 항목
 */
@Schema(description = "포인트 사용 내역 항목")
data class PointUsageHistoryItem(
    @field:Schema(description = "포인트 키", example = "point-key-12345")
    val pointKey: String,
    @field:Schema(description = "회원 ID", example = "12345")
    val memberId: Long,
    @field:Schema(description = "주문 번호", example = "ORDER-2025-01-01-001")
    val orderNumber: String,
    @field:Schema(description = "사용 금액", example = "10000")
    val totalAmount: Long,
    @field:Schema(description = "취소된 금액", example = "0")
    val cancelledAmount: Long,
    @field:Schema(description = "잔여 금액", example = "0")
    val remainingAmount: Long,
    @field:Schema(description = "상태", example = "USED")
    val status: String,
    @field:Schema(description = "생성 일시")
    val createdAt: LocalDateTime,
    @field:Schema(description = "수정 일시")
    val updatedAt: LocalDateTime
)

