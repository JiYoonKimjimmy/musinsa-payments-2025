package com.musinsa.payments.point.presentation.web.controller

import com.musinsa.payments.point.application.port.input.PointUsageUseCase
import com.musinsa.payments.point.presentation.web.dto.request.UsePointRequest
import com.musinsa.payments.point.presentation.web.dto.response.BaseResponse
import com.musinsa.payments.point.presentation.web.dto.response.UsePointResponse
import com.musinsa.payments.point.presentation.web.mapper.PointDtoMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 포인트 사용 컨트롤러
 * 포인트 사용 API를 제공합니다.
 */
@Tag(name = "포인트 사용", description = "포인트 사용 API")
@RequestMapping("/api/points/use")
@RestController
class PointUsageController(
    private val pointUsageUseCase: PointUsageUseCase
) {
    
    /**
     * 포인트 사용
     * POST /api/points/use
     */
    @Operation(
        summary = "포인트 사용",
        description = "주문 시 포인트를 사용합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사용 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UsePointResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 데이터 검증 실패 또는 잔액 부족",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    @PostMapping
    fun use(
        @Valid @RequestBody request: UsePointRequest
    ): BaseResponse<UsePointResponse> {
        val usage = pointUsageUseCase.use(
            memberId = request.memberId,
            orderNumber = request.orderNumber,
            amount = request.amount
        )
        
        val response = PointDtoMapper.toUsePointResponse(usage)
        return BaseResponse.success(response, "포인트가 성공적으로 사용되었습니다.")
    }
}

