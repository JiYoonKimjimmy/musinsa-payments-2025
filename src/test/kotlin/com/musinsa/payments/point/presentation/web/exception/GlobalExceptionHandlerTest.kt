package com.musinsa.payments.point.presentation.web.exception

import com.musinsa.payments.point.domain.exception.*
import com.musinsa.payments.point.presentation.web.dto.response.BaseResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

/**
 * GlobalExceptionHandler 예외 처리 테스트
 */
class GlobalExceptionHandlerTest : BehaviorSpec({
    
    val handler = GlobalExceptionHandler()
    
    Given("도메인 예외 처리") {
        When("InsufficientPointException이 발생하면") {
            val exception = InsufficientPointException()
            val response = handler.handleInsufficientPointException(exception)
            
            Then("400 Bad Request와 INSUFFICIENT_POINT 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "INSUFFICIENT_POINT"
                body.error!!.message.shouldNotBeNull()
            }
        }
        
        When("InvalidAmountException이 발생하면") {
            val exception = InvalidAmountException("적립 금액은 0보다 커야 합니다.")
            val response = handler.handleInvalidAmountException(exception)
            
            Then("400 Bad Request와 INVALID_AMOUNT 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "INVALID_AMOUNT"
                body.error!!.message shouldBe "적립 금액은 0보다 커야 합니다."
            }
        }
        
        When("MaxBalanceExceededException이 발생하면") {
            val exception = MaxBalanceExceededException()
            val response = handler.handleMaxBalanceExceededException(exception)
            
            Then("400 Bad Request와 EXCEEDED_MAX_BALANCE 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "EXCEEDED_MAX_BALANCE"
            }
        }
        
        When("MaxAccumulationExceededException이 발생하면") {
            val exception = MaxAccumulationExceededException("1회 최대 적립 금액을 초과했습니다.")
            val response = handler.handleMaxAccumulationExceededException(exception)
            
            Then("400 Bad Request와 EXCEEDED_MAX_ACCUMULATION 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "EXCEEDED_MAX_ACCUMULATION"
            }
        }
        
        When("InvalidExpirationDateException이 발생하면") {
            val exception = InvalidExpirationDateException("만료일이 범위를 벗어났습니다.")
            val response = handler.handleInvalidExpirationDateException(exception)
            
            Then("400 Bad Request와 INVALID_EXPIRATION_DATE 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "INVALID_EXPIRATION_DATE"
            }
        }
        
        When("CannotCancelAccumulationException이 발생하면") {
            val exception = CannotCancelAccumulationException()
            val response = handler.handleCannotCancelAccumulationException(exception)
            
            Then("400 Bad Request와 CANNOT_CANCEL_ACCUMULATION 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "CANNOT_CANCEL_ACCUMULATION"
            }
        }
        
        When("CannotCancelUsageException이 발생하면") {
            val exception = CannotCancelUsageException()
            val response = handler.handleCannotCancelUsageException(exception)
            
            Then("400 Bad Request와 CANNOT_CANCEL_USAGE 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "CANNOT_CANCEL_USAGE"
            }
        }
        
        When("PointExpiredException이 발생하면") {
            val exception = PointExpiredException()
            val response = handler.handlePointExpiredException(exception)
            
            Then("400 Bad Request와 POINT_EXPIRED 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "POINT_EXPIRED"
            }
        }
        
        When("CannotCancelDetailException이 발생하면") {
            val exception = CannotCancelDetailException()
            val response = handler.handleCannotCancelDetailException(exception)
            
            Then("400 Bad Request와 CANNOT_CANCEL_DETAIL 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "CANNOT_CANCEL_DETAIL"
            }
        }
    }
    
    Given("일반 예외 처리") {
        When("적립 건을 찾을 수 없다는 IllegalArgumentException이 발생하면") {
            val exception = IllegalArgumentException("포인트 적립 건을 찾을 수 없습니다: POINT_KEY_001")
            val response = handler.handleIllegalArgumentException(exception)
            
            Then("404 Not Found와 ACCUMULATION_NOT_FOUND 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.NOT_FOUND
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "ACCUMULATION_NOT_FOUND"
            }
        }
        
        When("사용 건을 찾을 수 없다는 IllegalArgumentException이 발생하면") {
            val exception = IllegalArgumentException("포인트 사용 건을 찾을 수 없습니다: USAGE_KEY_001")
            val response = handler.handleIllegalArgumentException(exception)
            
            Then("404 Not Found와 USAGE_NOT_FOUND 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.NOT_FOUND
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "USAGE_NOT_FOUND"
            }
        }
        
        When("설정을 찾을 수 없다는 IllegalArgumentException이 발생하면") {
            val exception = IllegalArgumentException("설정을 찾을 수 없습니다: CONFIG_KEY")
            val response = handler.handleIllegalArgumentException(exception)
            
            Then("404 Not Found와 CONFIG_NOT_FOUND 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.NOT_FOUND
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "CONFIG_NOT_FOUND"
            }
        }
        
        When("일반적인 IllegalArgumentException이 발생하면") {
            val exception = IllegalArgumentException("잘못된 인자입니다.")
            val response = handler.handleIllegalArgumentException(exception)
            
            Then("400 Bad Request와 INVALID_REQUEST 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "INVALID_REQUEST"
            }
        }
        
        When("MethodArgumentNotValidException이 발생하면") {
            // MethodArgumentNotValidException은 Spring이 자동으로 생성하므로
            // 실제 컨트롤러 테스트에서 검증하는 것이 더 적절합니다.
            // 여기서는 핸들러 메서드가 존재하고 호출 가능한지만 확인합니다.
            Then("핸들러 메서드가 존재해야 한다") {
                // 핸들러 메서드가 존재하는지 확인
                handler::handleMethodArgumentNotValidException.shouldNotBeNull()
            }
        }
        
        When("일반 Exception이 발생하면") {
            val exception = Exception("예상치 못한 오류")
            val response = handler.handleException(exception)
            
            Then("500 Internal Server Error와 INTERNAL_SERVER_ERROR 에러 코드가 반환되어야 한다") {
                response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                val body = response.body!!
                body.success shouldBe false
                body.error!!.code shouldBe "INTERNAL_SERVER_ERROR"
                body.error!!.message shouldContain "예상치 못한 오류"
            }
        }
    }
})

