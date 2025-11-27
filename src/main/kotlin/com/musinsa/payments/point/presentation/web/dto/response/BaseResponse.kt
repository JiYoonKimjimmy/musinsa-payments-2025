package com.musinsa.payments.point.presentation.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 공통 API 응답 래퍼 클래스
 * 모든 API 응답을 일관된 형식으로 래핑합니다.
 */
@Schema(description = "공통 API 응답")
data class BaseResponse<T>(
    @field:Schema(description = "성공 여부", example = "true")
    val success: Boolean,
    @field:Schema(description = "응답 데이터")
    val data: T? = null,
    @field:Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
    val message: String? = null,
    @field:Schema(description = "에러 정보")
    val error: ErrorResponse? = null
) {
    companion object {
        /**
         * 성공 응답 생성
         */
        fun <T> success(data: T, message: String? = null): BaseResponse<T> {
            return BaseResponse(
                success = true,
                data = data,
                message = message
            )
        }
        
        /**
         * 실패 응답 생성
         */
        fun <T> error(code: String, message: String): BaseResponse<T> {
            return BaseResponse(
                success = false,
                error = ErrorResponse(
                    code = code,
                    message = message
                )
            )
        }
    }
}

/**
 * 에러 응답 DTO
 */
@Schema(description = "에러 응답")
data class ErrorResponse(
    @field:Schema(description = "에러 코드", example = "INSUFFICIENT_POINT")
    val code: String,
    @field:Schema(description = "에러 메시지", example = "포인트 잔액이 부족합니다.")
    val message: String
)

