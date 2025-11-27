package com.musinsa.payments.point.presentation.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/**
 * 포인트 잔액 응답 DTO
 */
@Schema(description = "포인트 잔액 응답")
data class PointBalanceResponse(
    @field:Schema(description = "회원 ID", example = "12345")
    val memberId: Long,
    @field:Schema(description = "전체 잔액", example = "100000")
    val totalBalance: Long,
    @field:Schema(description = "사용 가능 잔액", example = "95000")
    val availableBalance: Long,
    @field:Schema(description = "만료된 잔액", example = "5000")
    val expiredBalance: Long,
    @field:Schema(description = "적립 내역 목록")
    val accumulations: List<PointAccumulationItem>
)

/**
 * 포인트 적립 내역 항목
 */
@Schema(description = "포인트 적립 내역 항목")
data class PointAccumulationItem(
    @field:Schema(description = "포인트 키", example = "point-key-12345")
    val pointKey: String,
    @field:Schema(description = "적립 금액", example = "10000")
    val amount: Long,
    @field:Schema(description = "사용 가능 금액", example = "9500")
    val availableAmount: Long,
    @field:Schema(description = "만료일")
    val expirationDate: LocalDate,
    @field:Schema(description = "수동 지급 여부", example = "false")
    val isManualGrant: Boolean,
    @field:Schema(description = "상태", example = "ACCUMULATED")
    val status: String
)

