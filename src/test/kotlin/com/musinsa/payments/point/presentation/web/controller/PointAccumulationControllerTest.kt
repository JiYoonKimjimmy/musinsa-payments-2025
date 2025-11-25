package com.musinsa.payments.point.presentation.web.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.musinsa.payments.point.application.port.input.PointAccumulationUseCase
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.domain.exception.CannotCancelAccumulationException
import com.musinsa.payments.point.domain.exception.MaxBalanceExceededException
import com.musinsa.payments.point.domain.valueobject.Money
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
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * PointAccumulationController REST API 테스트
 */
class PointAccumulationControllerTest : BehaviorSpec({
    
    val pointAccumulationUseCase = mockk<PointAccumulationUseCase>()
    val controller = PointAccumulationController(pointAccumulationUseCase)
    val exceptionHandler = GlobalExceptionHandler()
    val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(exceptionHandler)
        .build()
    val objectMapper = ObjectMapper()
    
    Given("포인트 적립 API") {
        When("유효한 요청으로 포인트를 적립하면") {
            val request = mapOf(
                "memberId" to 1L,
                "amount" to 1000L,
                "expirationDays" to 365,
                "isManualGrant" to false
            )
            
            val accumulation = PointAccumulation(
                pointKey = "POINT_KEY_001",
                memberId = 1L,
                amount = Money.of(1000L),
                expirationDate = LocalDate.now().plusDays(365),
                isManualGrant = false,
                status = PointAccumulationStatus.ACCUMULATED,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            
            every { pointAccumulationUseCase.accumulate(1L, 1000L, 365, false) } returns accumulation
            
            val result = mockMvc.perform(
                post("/api/points/accumulate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            
            Then("적립이 성공하고 응답이 반환되어야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe true
                responseBody["data"]["pointKey"].asText() shouldBe "POINT_KEY_001"
                responseBody["data"]["memberId"].asLong() shouldBe 1L
                responseBody["data"]["amount"].asLong() shouldBe 1000L
                responseBody["data"]["availableAmount"].asLong() shouldBe 1000L
                responseBody["message"].asText() shouldNotBe null
                
                verify { pointAccumulationUseCase.accumulate(1L, 1000L, 365, false) }
            }
        }
        
        When("필수 필드가 누락된 요청을 보내면") {
            val request = mapOf(
                "amount" to 1000L
                // memberId 누락
            )
            
            val result = mockMvc.perform(
                post("/api/points/accumulate")
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
        
        When("최대 보유 금액을 초과하는 요청을 보내면") {
            val request = mapOf(
                "memberId" to 1L,
                "amount" to 10000001L,
                "expirationDays" to 365
            )
            
            every { 
                pointAccumulationUseCase.accumulate(1L, 10000001L, 365, false) 
            } throws MaxBalanceExceededException()
            
            val result = mockMvc.perform(
                post("/api/points/accumulate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            
            Then("에러 응답이 반환되어야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe false
                responseBody["error"]["code"].asText() shouldBe "EXCEEDED_MAX_BALANCE"
            }
        }
    }
    
    Given("포인트 적립 취소 API") {
        When("유효한 요청으로 포인트 적립을 취소하면") {
            val pointKey = "POINT_KEY_001"
            val request = mapOf(
                "reason" to "적립 오류"
            )
            
            val accumulation = PointAccumulation(
                pointKey = pointKey,
                memberId = 1L,
                amount = Money.of(1000L),
                expirationDate = LocalDate.now().plusDays(365),
                isManualGrant = false,
                status = PointAccumulationStatus.CANCELLED,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            
            every { 
                pointAccumulationUseCase.cancelAccumulation(pointKey, "적립 오류") 
            } returns accumulation
            
            val result = mockMvc.perform(
                post("/api/points/accumulate/$pointKey/cancel")
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
                responseBody["data"]["status"].asText() shouldBe "CANCELLED"
                responseBody["message"].asText() shouldNotBe null
                
                verify { pointAccumulationUseCase.cancelAccumulation(pointKey, "적립 오류") }
            }
        }
        
        When("이미 사용된 포인트를 취소하려고 하면") {
            val pointKey = "POINT_KEY_001"
            val request = mapOf(
                "reason" to "적립 오류"
            )
            
            every { 
                pointAccumulationUseCase.cancelAccumulation(pointKey, "적립 오류") 
            } throws CannotCancelAccumulationException()
            
            val result = mockMvc.perform(
                post("/api/points/accumulate/$pointKey/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            
            Then("에러 응답이 반환되어야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe false
                responseBody["error"]["code"].asText() shouldBe "CANNOT_CANCEL_ACCUMULATION"
            }
        }
        
        When("존재하지 않는 적립 건을 취소하려고 하면") {
            val pointKey = "NON_EXISTENT_KEY"
            val request = mapOf(
                "reason" to "적립 오류"
            )
            
            every { 
                pointAccumulationUseCase.cancelAccumulation(pointKey, "적립 오류") 
            } throws IllegalArgumentException("포인트 적립 건을 찾을 수 없습니다: $pointKey")
            
            val result = mockMvc.perform(
                post("/api/points/accumulate/$pointKey/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andReturn()
            
            Then("404 에러 응답이 반환되어야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe false
                responseBody["error"]["code"].asText() shouldBe "ACCUMULATION_NOT_FOUND"
            }
        }
    }
})

