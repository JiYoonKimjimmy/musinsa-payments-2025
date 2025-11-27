package com.musinsa.payments.point.presentation.web.controller

import com.musinsa.payments.point.application.service.PointBalanceReconciliationService
import com.musinsa.payments.point.presentation.web.dto.response.BaseResponse
import com.musinsa.payments.point.presentation.web.dto.response.ReconciliationResultResponse
import com.musinsa.payments.point.presentation.web.dto.response.ReconciliationSummaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 포인트 잔액 정합성 보정 관리 컨트롤러
 * 회원별 잔액 테이블과 실제 적립 건의 잔액을 비교하여 불일치 시 보정하는 Admin API를 제공합니다.
 */
@Tag(name = "포인트 정합성 보정 관리", description = "포인트 잔액 정합성 검증 및 보정 API")
@RequestMapping("/api/admin/points/reconciliation")
@RestController
class PointReconciliationController(
    private val pointBalanceReconciliationService: PointBalanceReconciliationService
) {
    
    /**
     * 특정 회원의 잔액 정합성 보정
     * POST /api/admin/points/reconciliation/members/{memberId}
     */
    @Operation(
        summary = "특정 회원 잔액 정합성 보정",
        description = "특정 회원의 캐시된 잔액과 실제 적립 건 합계를 비교하여 불일치 시 보정합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정합성 검증 및 보정 완료",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ReconciliationResultResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    @PostMapping("/members/{memberId}")
    fun reconcileMemberBalance(
        @Parameter(description = "회원 ID", required = true, example = "12345")
        @PathVariable memberId: Long
    ): BaseResponse<ReconciliationResultResponse> {
        val result = pointBalanceReconciliationService.reconcileMemberBalance(memberId)
        val response = ReconciliationResultResponse.from(result)
        
        val message = when (result.status) {
            PointBalanceReconciliationService.ReconciliationStatus.MATCHED -> 
                "잔액이 일치합니다."
            PointBalanceReconciliationService.ReconciliationStatus.CORRECTED -> 
                "잔액 불일치가 발견되어 보정되었습니다. 차이: ${result.difference.amount}원"
            PointBalanceReconciliationService.ReconciliationStatus.CREATED -> 
                "잔액 캐시가 신규 생성되었습니다."
            PointBalanceReconciliationService.ReconciliationStatus.SKIPPED -> 
                "잔액이 없어 건너뛰었습니다."
        }
        
        return BaseResponse.success(response, message)
    }
    
    /**
     * 모든 회원의 잔액 정합성 보정
     * POST /api/admin/points/reconciliation/all
     */
    @Operation(
        summary = "전체 회원 잔액 정합성 보정",
        description = """
            모든 회원의 캐시된 잔액과 실제 적립 건 합계를 비교하여 불일치 시 보정합니다.
            
            ⚠️ 주의: 회원 수가 많은 경우 처리 시간이 오래 걸릴 수 있습니다.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "전체 정합성 검증 및 보정 완료",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ReconciliationSummaryResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    @PostMapping("/all")
    fun reconcileAllBalances(): BaseResponse<ReconciliationSummaryResponse> {
        val results = pointBalanceReconciliationService.reconcileAllBalances()
        val response = ReconciliationSummaryResponse.from(results)
        
        val message = buildString {
            append("정합성 검증 완료. ")
            append("총 ${response.totalChecked}건 검사, ")
            append("${response.matchedCount}건 일치, ")
            append("${response.correctedCount}건 보정, ")
            append("${response.createdCount}건 신규 생성, ")
            append("${response.skippedCount}건 건너뜀.")
        }
        
        return BaseResponse.success(response, message)
    }
}

