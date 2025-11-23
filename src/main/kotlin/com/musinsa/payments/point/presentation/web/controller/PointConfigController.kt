package com.musinsa.payments.point.presentation.web.controller

import com.musinsa.payments.point.application.service.PointConfigService
import com.musinsa.payments.point.domain.exception.ConfigNotFoundException
import com.musinsa.payments.point.presentation.web.dto.request.UpdateConfigRequest
import com.musinsa.payments.point.presentation.web.dto.response.BaseResponse
import com.musinsa.payments.point.presentation.web.dto.response.PointConfigResponse
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
 * 포인트 설정 관리 컨트롤러
 * 포인트 설정 조회 및 업데이트 API를 제공합니다.
 */
@Tag(name = "포인트 설정 관리", description = "포인트 설정 조회 및 업데이트 API")
@RequestMapping("/api/admin/points/config")
@RestController
class PointConfigController(
    private val pointConfigService: PointConfigService
) {
    
    /**
     * 모든 설정 조회
     * GET /api/admin/points/config
     */
    @Operation(
        summary = "모든 설정 조회",
        description = "포인트 시스템의 모든 설정을 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PointConfigResponse::class)
                )]
            )
        ]
    )
    @GetMapping
    fun getAllConfigs(): BaseResponse<List<PointConfigResponse>> {
        val configs = pointConfigService.findAll()
        val response = PointDtoMapper.toPointConfigListResponse(configs)
        return BaseResponse.success(response, "설정 조회에 성공했습니다.")
    }
    
    /**
     * 특정 설정 조회
     * GET /api/admin/points/config/{configKey}
     */
    @Operation(
        summary = "특정 설정 조회",
        description = "설정 키로 특정 설정을 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PointConfigResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "설정을 찾을 수 없음",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    @GetMapping("/{configKey}")
    fun getConfig(
        @Parameter(description = "설정 키", required = true)
        @PathVariable configKey: String
    ): BaseResponse<PointConfigResponse> {
        val config = pointConfigService.findByConfigKey(configKey)
            .orElseThrow { ConfigNotFoundException("설정을 찾을 수 없습니다: $configKey") }
        
        val response = PointDtoMapper.toPointConfigResponse(config)
        return BaseResponse.success(response, "설정 조회에 성공했습니다.")
    }
    
    /**
     * 설정 업데이트
     * PUT /api/admin/points/config/{configKey}
     */
    @Operation(
        summary = "설정 업데이트",
        description = "특정 설정 값을 업데이트합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "업데이트 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PointConfigResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 데이터 검증 실패 또는 설정 값이 유효하지 않음",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "404",
                description = "설정을 찾을 수 없음",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    @PutMapping("/{configKey}")
    fun updateConfig(
        @Parameter(description = "설정 키", required = true)
        @PathVariable configKey: String,
        @Valid @RequestBody request: UpdateConfigRequest
    ): BaseResponse<PointConfigResponse> {
        val updatedConfig = pointConfigService.updateConfig(
            configKey = configKey,
            configValue = request.configValue,
            description = request.description,
            changedBy = null // TODO: 실제 사용자 정보를 주입받아야 함
        )
        
        val response = PointDtoMapper.toPointConfigResponse(updatedConfig)
        return BaseResponse.success(response, "설정이 성공적으로 업데이트되었습니다.")
    }
}

