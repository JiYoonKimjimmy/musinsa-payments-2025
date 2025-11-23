package com.musinsa.payments.point.presentation.web.dto.response

/**
 * 공통 API 응답 래퍼 클래스
 * 모든 API 응답을 일관된 형식으로 래핑합니다.
 */
data class BaseResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
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
data class ErrorResponse(
    val code: String,
    val message: String
)

