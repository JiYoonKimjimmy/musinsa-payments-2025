package com.musinsa.payments.point.presentation.web.controller

import com.musinsa.payments.point.application.port.input.PointAccumulationUseCase
import com.musinsa.payments.point.presentation.web.dto.request.AccumulatePointRequest
import com.musinsa.payments.point.presentation.web.dto.request.CancelAccumulationRequest
import com.musinsa.payments.point.presentation.web.dto.response.AccumulatePointResponse
import com.musinsa.payments.point.presentation.web.dto.response.BaseResponse
import com.musinsa.payments.point.presentation.web.dto.response.CancelAccumulationResponse
import com.musinsa.payments.point.presentation.web.mapper.PointDtoMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * 포인트 적립 컨트롤러
 * 포인트 적립 및 적립 취소 API를 제공합니다.
 */
@Tag(name = "포인트 적립", description = "포인트 적립 및 적립 취소 API")
@RequestMapping("/api/points/accumulate")
@RestController
class PointAccumulationController(
    private val pointAccumulationUseCase: PointAccumulationUseCase
) {
    
    /**
     * 포인트 적립
     * POST /api/points/accumulate
     */
    @Operation(
        summary = "포인트 적립",
        description = "사용자에게 포인트를 적립합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "적립 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = AccumulatePointResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 데이터 검증 실패 또는 비즈니스 규칙 위반",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    @PostMapping
    fun accumulate(
        @Valid @RequestBody request: AccumulatePointRequest
    ): BaseResponse<AccumulatePointResponse> {
        val accumulation = pointAccumulationUseCase.accumulate(
            memberId = request.memberId,
            amount = request.amount,
            expirationDays = request.expirationDays,
            isManualGrant = request.isManualGrant
        )
        
        val response = PointDtoMapper.toAccumulatePointResponse(accumulation)
        return BaseResponse.success(response, "포인트가 성공적으로 적립되었습니다.")
    }
    
    /**
     * 포인트 적립 취소
     * POST /api/points/accumulate/{pointKey}/cancel
     */
    @Operation(
        summary = "포인트 적립 취소",
        description = "특정 적립 건을 취소합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "취소 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CancelAccumulationResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "취소 불가 (이미 사용된 포인트 등)",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "404",
                description = "적립 건을 찾을 수 없음",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    @PostMapping("/{pointKey}/cancel")
    fun cancelAccumulation(
        @Parameter(description = "취소할 적립 건의 포인트 키", required = true)
        @PathVariable pointKey: String,
        @Valid @RequestBody request: CancelAccumulationRequest
    ): BaseResponse<CancelAccumulationResponse> {
        val accumulation = pointAccumulationUseCase.cancelAccumulation(
            pointKey = pointKey,
            reason = request.reason
        )
        
        val response = PointDtoMapper.toCancelAccumulationResponse(accumulation)
        return BaseResponse.success(response, "포인트 적립이 성공적으로 취소되었습니다.")
    }
}

