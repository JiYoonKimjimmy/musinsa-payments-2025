package com.musinsa.payments.point.presentation.web.controller

import com.musinsa.payments.point.application.port.input.PointCancellationUseCase
import com.musinsa.payments.point.presentation.web.dto.request.CancelUsageRequest
import com.musinsa.payments.point.presentation.web.dto.response.BaseResponse
import com.musinsa.payments.point.presentation.web.dto.response.CancelUsageResponse
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
 * 포인트 사용 취소 컨트롤러
 * 포인트 사용 취소 API를 제공합니다.
 */
@Tag(name = "포인트 사용 취소", description = "포인트 사용 취소 API")
@RequestMapping("/api/points/use")
@RestController
class PointCancellationController(
    private val pointCancellationUseCase: PointCancellationUseCase
) {
    
    /**
     * 포인트 사용 취소
     * POST /api/points/use/{pointKey}/cancel
     */
    @Operation(
        summary = "포인트 사용 취소",
        description = "사용한 포인트를 취소합니다. 전체 또는 부분 취소가 가능합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "취소 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CancelUsageResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "취소 불가 (이미 취소된 사용 건 등)",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용 건을 찾을 수 없음",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    @PostMapping("/{pointKey}/cancel")
    fun cancelUsage(
        @Parameter(description = "취소할 사용 건의 포인트 키", required = true)
        @PathVariable pointKey: String,
        @Valid @RequestBody request: CancelUsageRequest
    ): BaseResponse<CancelUsageResponse> {
        val usage = pointCancellationUseCase.cancelUsage(
            pointKey = pointKey,
            amount = request.amount,
            reason = request.reason
        )
        
        val response = PointDtoMapper.toCancelUsageResponse(usage)
        return BaseResponse.success(response, "포인트 사용이 성공적으로 취소되었습니다.")
    }
}

