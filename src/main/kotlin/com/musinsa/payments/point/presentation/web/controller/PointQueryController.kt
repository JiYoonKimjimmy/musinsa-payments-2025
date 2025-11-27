package com.musinsa.payments.point.presentation.web.controller

import com.musinsa.payments.point.application.port.input.PointQueryUseCase
import com.musinsa.payments.point.presentation.web.dto.response.BaseResponse
import com.musinsa.payments.point.presentation.web.dto.response.PointBalanceResponse
import com.musinsa.payments.point.presentation.web.dto.response.PointUsageHistoryResponse
import com.musinsa.payments.point.presentation.web.mapper.PointDtoMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/**
 * 포인트 조회 컨트롤러
 * 포인트 잔액 조회 및 사용 내역 조회 API를 제공합니다.
 */
@Tag(name = "포인트 조회", description = "포인트 잔액 조회 및 사용 내역 조회 API")
@RequestMapping("/api/points")
@RestController
class PointQueryController(
    private val pointQueryUseCase: PointQueryUseCase
) {
    
    /**
     * 포인트 잔액 조회
     * GET /api/points/balance/{memberId}
     */
    @Operation(
        summary = "포인트 잔액 조회",
        description = "사용자의 포인트 잔액을 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PointBalanceResponse::class)
                )]
            )
        ]
    )
    @GetMapping("/balance/{memberId}")
    suspend fun getBalance(
        @Parameter(description = "회원 ID", required = true)
        @PathVariable memberId: Long
    ): BaseResponse<PointBalanceResponse> {
        val balanceResult = pointQueryUseCase.getBalance(memberId)

        val response = PointDtoMapper.toPointBalanceResponse(
            memberId = balanceResult.memberId,
            totalBalance = balanceResult.totalBalance,
            availableBalance = balanceResult.availableBalance,
            expiredBalance = balanceResult.expiredBalance,
            accumulations = balanceResult.accumulations
        )

        return BaseResponse.success(response, "포인트 잔액 조회가 완료되었습니다.")
    }
    
    /**
     * 포인트 사용 내역 조회
     * GET /api/points/history/{memberId}
     */
    @Operation(
        summary = "포인트 사용 내역 조회",
        description = "사용자의 포인트 사용 내역을 조회합니다. 주문번호로 필터링할 수 있습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PointUsageHistoryResponse::class)
                )]
            )
        ]
    )
    @GetMapping("/history/{memberId}")
    suspend fun getUsageHistory(
        @Parameter(description = "회원 ID", required = true)
        @PathVariable memberId: Long,
        @Parameter(description = "주문번호 (옵션, 필터링용)")
        @RequestParam(required = false) orderNumber: String?,
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): BaseResponse<PointUsageHistoryResponse> {
        val pageable: Pageable = PageRequest.of(page, size)
        val usagePage = pointQueryUseCase.getUsageHistory(
            memberId = memberId,
            orderNumber = orderNumber,
            pageable = pageable
        )

        val response = PointDtoMapper.toPointUsageHistoryResponse(usagePage)
        return BaseResponse.success(response, "포인트 사용 내역 조회가 완료되었습니다.")
    }
}

