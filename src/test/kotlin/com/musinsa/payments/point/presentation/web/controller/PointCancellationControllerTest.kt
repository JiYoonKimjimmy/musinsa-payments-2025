package com.musinsa.payments.point.presentation.web.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.musinsa.payments.point.application.port.input.PointCancellationUseCase
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.entity.PointUsageStatus
import com.musinsa.payments.point.domain.exception.CannotCancelUsageException
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import com.musinsa.payments.point.presentation.web.exception.GlobalExceptionHandler
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

/**
 * PointCancellationController REST API 테스트
 */
class PointCancellationControllerTest : BehaviorSpec({
    
    val pointCancellationUseCase = mockk<PointCancellationUseCase>()
    val controller = PointCancellationController(pointCancellationUseCase)
    val exceptionHandler = GlobalExceptionHandler()
    val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(exceptionHandler)
        .build()
    val objectMapper = ObjectMapper()
    
    Given("포인트 사용 취소 API") {
        When("유효한 요청으로 전체 취소하면") {
            val pointKey = "USAGE_KEY_001"
            val request = mapOf(
                "reason" to "주문 취소"
                // amount가 null이면 전체 취소
            )
            
            val usage = PointUsage(
                pointKey = pointKey,
                memberId = 1L,
                orderNumber = OrderNumber.of("ORDER_001"),
                totalAmount = Money.of(1000L),
                cancelledAmount = Money.of(1000L),
                status = PointUsageStatus.FULLY_CANCELLED,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            
            every { 
                pointCancellationUseCase.cancelUsage(pointKey, null, "주문 취소") 
            } returns usage
            
            val result = mockMvc.perform(
                post("/api/points/use/$pointKey/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            
            Then("취소가 성공하고 응답이 반환되어야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe true
                responseBody["data"]["pointKey"].asText() shouldBe pointKey
                responseBody["data"]["status"].asText() shouldBe "FULLY_CANCELLED"
                responseBody["data"]["cancelledAmount"].asLong() shouldBe 1000L
                responseBody["message"].asText() shouldNotBe null
                
                verify { pointCancellationUseCase.cancelUsage(pointKey, null, "주문 취소") }
            }
        }
        
        When("유효한 요청으로 부분 취소하면") {
            val pointKey = "USAGE_KEY_001"
            val request = mapOf(
                "amount" to 500L,
                "reason" to "부분 취소"
            )
            
            val usage = PointUsage(
                pointKey = pointKey,
                memberId = 1L,
                orderNumber = OrderNumber.of("ORDER_001"),
                totalAmount = Money.of(1000L),
                cancelledAmount = Money.of(500L),
                status = PointUsageStatus.PARTIALLY_CANCELLED,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            
            every { 
                pointCancellationUseCase.cancelUsage(pointKey, 500L, "부분 취소") 
            } returns usage
            
            val result = mockMvc.perform(
                post("/api/points/use/$pointKey/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            
            Then("부분 취소가 성공하고 응답이 반환되어야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe true
                responseBody["data"]["pointKey"].asText() shouldBe pointKey
                responseBody["data"]["cancelledAmount"].asLong() shouldBe 500L
                responseBody["message"].asText() shouldNotBe null
                
                verify { pointCancellationUseCase.cancelUsage(pointKey, 500L, "부분 취소") }
            }
        }
        
        When("이미 취소된 사용 건을 취소하려고 하면") {
            val pointKey = "USAGE_KEY_001"
            val request = mapOf(
                "reason" to "주문 취소"
            )
            
            every { 
                pointCancellationUseCase.cancelUsage(pointKey, null, "주문 취소") 
            } throws CannotCancelUsageException()
            
            val result = mockMvc.perform(
                post("/api/points/use/$pointKey/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            
            Then("에러 응답이 반환되어야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe false
                responseBody["error"]["code"].asText() shouldBe "CANNOT_CANCEL_USAGE"
            }
        }
        
        When("존재하지 않는 사용 건을 취소하려고 하면") {
            val pointKey = "NON_EXISTENT_KEY"
            val request = mapOf(
                "reason" to "주문 취소"
            )
            
            every { 
                pointCancellationUseCase.cancelUsage(pointKey, null, "주문 취소") 
            } throws IllegalArgumentException("포인트 사용 건을 찾을 수 없습니다: $pointKey")
            
            val result = mockMvc.perform(
                post("/api/points/use/$pointKey/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andReturn()
            
            Then("404 에러 응답이 반환되어야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe false
                responseBody["error"]["code"].asText() shouldBe "USAGE_NOT_FOUND"
            }
        }
        
        When("0원 이하의 금액으로 취소하려고 하면") {
            val pointKey = "USAGE_KEY_001"
            val request = mapOf(
                "amount" to 0L,
                "reason" to "주문 취소"
            )
            
            val result = mockMvc.perform(
                post("/api/points/use/$pointKey/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            
            Then("검증 오류가 발생해야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe false
                responseBody["error"]["code"].asText() shouldBe "INVALID_REQUEST"
            }
        }
    }
})

