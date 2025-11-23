package com.musinsa.payments.point.presentation.web.exception

import com.musinsa.payments.point.domain.exception.*
import com.musinsa.payments.point.presentation.web.dto.response.BaseResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 전역 예외 처리 핸들러
 * 모든 컨트롤러에서 발생하는 예외를 일괄 처리합니다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    
    // ========== 도메인 예외 처리 ==========
    
    /**
     * 잔액 부족 예외 처리
     */
    @ExceptionHandler(InsufficientPointException::class)
    fun handleInsufficientPointException(e: InsufficientPointException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error("INSUFFICIENT_POINT", e.message ?: "사용 가능한 포인트가 부족합니다."))
    }
    
    /**
     * 잘못된 금액 예외 처리
     */
    @ExceptionHandler(InvalidAmountException::class)
    fun handleInvalidAmountException(e: InvalidAmountException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error("INVALID_AMOUNT", e.message ?: "잘못된 금액입니다."))
    }
    
    /**
     * 최대 보유 금액 초과 예외 처리
     */
    @ExceptionHandler(MaxBalanceExceededException::class)
    fun handleMaxBalanceExceededException(e: MaxBalanceExceededException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error("EXCEEDED_MAX_BALANCE", e.message ?: "개인별 최대 보유 금액을 초과했습니다."))
    }
    
    /**
     * 최대 적립 금액 초과 예외 처리
     */
    @ExceptionHandler(MaxAccumulationExceededException::class)
    fun handleMaxAccumulationExceededException(e: MaxAccumulationExceededException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error("EXCEEDED_MAX_ACCUMULATION", e.message ?: "1회 최대 적립 금액을 초과했습니다."))
    }
    
    /**
     * 잘못된 만료일 예외 처리
     */
    @ExceptionHandler(InvalidExpirationDateException::class)
    fun handleInvalidExpirationDateException(e: InvalidExpirationDateException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error("INVALID_EXPIRATION_DATE", e.message ?: "만료일이 범위를 벗어났습니다."))
    }
    
    /**
     * 적립 취소 불가 예외 처리
     */
    @ExceptionHandler(CannotCancelAccumulationException::class)
    fun handleCannotCancelAccumulationException(e: CannotCancelAccumulationException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error("CANNOT_CANCEL_ACCUMULATION", e.message ?: "이미 사용된 포인트는 적립 취소할 수 없습니다."))
    }
    
    /**
     * 사용 취소 불가 예외 처리
     */
    @ExceptionHandler(CannotCancelUsageException::class)
    fun handleCannotCancelUsageException(e: CannotCancelUsageException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error("CANNOT_CANCEL_USAGE", e.message ?: "취소할 수 없는 사용 건입니다."))
    }
    
    /**
     * 만료된 포인트 예외 처리
     */
    @ExceptionHandler(PointExpiredException::class)
    fun handlePointExpiredException(e: PointExpiredException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error("POINT_EXPIRED", e.message ?: "만료된 포인트입니다."))
    }
    
    /**
     * 상세 내역 취소 불가 예외 처리
     */
    @ExceptionHandler(CannotCancelDetailException::class)
    fun handleCannotCancelDetailException(e: CannotCancelDetailException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error("CANNOT_CANCEL_DETAIL", e.message ?: "상세 내역을 취소할 수 없습니다."))
    }
    
    /**
     * 설정을 찾을 수 없음 예외 처리
     */
    @ExceptionHandler(ConfigNotFoundException::class)
    fun handleConfigNotFoundException(e: ConfigNotFoundException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(BaseResponse.error("CONFIG_NOT_FOUND", e.message ?: "설정을 찾을 수 없습니다."))
    }
    
    /**
     * 유효하지 않은 설정 값 예외 처리
     */
    @ExceptionHandler(InvalidConfigValueException::class)
    fun handleInvalidConfigValueException(e: InvalidConfigValueException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error("INVALID_CONFIG_VALUE", e.message ?: "설정 값이 유효하지 않습니다."))
    }
    
    /**
     * 유효하지 않은 설정 키 예외 처리
     */
    @ExceptionHandler(InvalidConfigKeyException::class)
    fun handleInvalidConfigKeyException(e: InvalidConfigKeyException): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error("INVALID_CONFIG_KEY", e.message ?: "설정 키가 유효하지 않습니다."))
    }
    
    // ========== 일반 예외 처리 ==========
    
    /**
     * 잘못된 인자 예외 처리
     * 적립 건/사용 건을 찾을 수 없을 때 발생
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<BaseResponse<Nothing>> {
        val message = e.message ?: "잘못된 요청입니다."
        
        // 메시지에 따라 에러 코드 결정
        val errorCode = when {
            message.contains("적립 건을 찾을 수 없습니다") -> "ACCUMULATION_NOT_FOUND"
            message.contains("사용 건을 찾을 수 없습니다") -> "USAGE_NOT_FOUND"
            message.contains("설정을 찾을 수 없습니다") -> "CONFIG_NOT_FOUND"
            else -> "INVALID_REQUEST"
        }
        
        val httpStatus = when (errorCode) {
            "ACCUMULATION_NOT_FOUND", "USAGE_NOT_FOUND", "CONFIG_NOT_FOUND" -> HttpStatus.NOT_FOUND
            else -> HttpStatus.BAD_REQUEST
        }
        
        return ResponseEntity
            .status(httpStatus)
            .body(BaseResponse.error(errorCode, message))
    }
    
    /**
     * 검증 오류 예외 처리
     * @Valid 어노테이션 검증 실패 시 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<BaseResponse<Nothing>> {
        val errors = e.bindingResult.fieldErrors
        val errorMessage = errors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BaseResponse.error("INVALID_REQUEST", "요청 데이터 검증 실패: $errorMessage"))
    }
    
    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<BaseResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(BaseResponse.error("INTERNAL_SERVER_ERROR", "내부 서버 오류가 발생했습니다: ${e.message}"))
    }
}

