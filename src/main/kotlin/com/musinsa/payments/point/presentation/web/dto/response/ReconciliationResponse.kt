package com.musinsa.payments.point.presentation.web.dto.response

import com.musinsa.payments.point.application.service.PointBalanceReconciliationService.ReconciliationResult
import com.musinsa.payments.point.application.service.PointBalanceReconciliationService.ReconciliationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

/**
 * 정합성 보정 결과 응답 DTO
 */
@Schema(description = "정합성 보정 결과")
data class ReconciliationResultResponse(
    @Schema(description = "회원 ID", example = "12345")
    val memberId: Long,
    
    @Schema(description = "보정 상태", example = "CORRECTED")
    val status: String,
    
    @Schema(description = "실제 잔액 (적립 건 합계)", example = "50000")
    val actualBalance: BigDecimal,
    
    @Schema(description = "캐시된 잔액 (잔액 테이블)", example = "45000")
    val cachedBalance: BigDecimal,
    
    @Schema(description = "차이 금액", example = "5000")
    val difference: BigDecimal
) {
    companion object {
        fun from(result: ReconciliationResult): ReconciliationResultResponse {
            return ReconciliationResultResponse(
                memberId = result.memberId,
                status = result.status.name,
                actualBalance = result.actualBalance.amount,
                cachedBalance = result.cachedBalance.amount,
                difference = result.difference.amount
            )
        }
    }
}

/**
 * 전체 정합성 보정 결과 요약 응답 DTO
 */
@Schema(description = "전체 정합성 보정 결과 요약")
data class ReconciliationSummaryResponse(
    @Schema(description = "총 검사 건수", example = "100")
    val totalChecked: Int,
    
    @Schema(description = "일치 건수", example = "95")
    val matchedCount: Int,
    
    @Schema(description = "보정된 건수", example = "3")
    val correctedCount: Int,
    
    @Schema(description = "신규 생성 건수", example = "1")
    val createdCount: Int,
    
    @Schema(description = "건너뛴 건수", example = "1")
    val skippedCount: Int,
    
    @Schema(description = "상세 결과 목록")
    val details: List<ReconciliationResultResponse>
) {
    companion object {
        fun from(results: List<ReconciliationResult>): ReconciliationSummaryResponse {
            return ReconciliationSummaryResponse(
                totalChecked = results.size,
                matchedCount = results.count { it.status == ReconciliationStatus.MATCHED },
                correctedCount = results.count { it.status == ReconciliationStatus.CORRECTED },
                createdCount = results.count { it.status == ReconciliationStatus.CREATED },
                skippedCount = results.count { it.status == ReconciliationStatus.SKIPPED },
                details = results.map { ReconciliationResultResponse.from(it) }
            )
        }
    }
}

