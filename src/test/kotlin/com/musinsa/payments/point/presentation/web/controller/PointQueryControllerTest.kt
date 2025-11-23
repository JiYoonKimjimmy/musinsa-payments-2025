package com.musinsa.payments.point.presentation.web.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.musinsa.payments.point.application.port.input.PointBalanceResult
import com.musinsa.payments.point.application.port.input.PointQueryUseCase
import com.musinsa.payments.point.presentation.web.exception.GlobalExceptionHandler
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * PointQueryController REST API 테스트
 */
class PointQueryControllerTest : BehaviorSpec({
    
    val pointQueryUseCase = mockk<PointQueryUseCase>()
    val controller = PointQueryController(pointQueryUseCase)
    val exceptionHandler = GlobalExceptionHandler()
    val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(exceptionHandler)
        .build()
    val objectMapper = ObjectMapper()
    
    Given("포인트 잔액 조회 API") {
        When("유효한 회원 ID로 잔액을 조회하면") {
            val memberId = 1L
            
            val accumulation1 = PointAccumulation(
                pointKey = "POINT_KEY_001",
                memberId = memberId,
                amount = Money.of(1000L),
                expirationDate = LocalDate.now().plusDays(365),
                isManualGrant = false,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            
            val accumulation2 = PointAccumulation(
                pointKey = "POINT_KEY_002",
                memberId = memberId,
                amount = Money.of(500L),
                expirationDate = LocalDate.now().plusDays(180),
                isManualGrant = false,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            
            val balanceResult = PointBalanceResult(
                memberId = memberId,
                totalBalance = 1500L,
                availableBalance = 1500L,
                expiredBalance = 0L,
                accumulations = listOf(accumulation1, accumulation2)
            )
            
            every { pointQueryUseCase.getBalance(memberId) } returns balanceResult
            
            val result = mockMvc.perform(
                get("/api/points/balance/$memberId")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            
            Then("잔액 정보가 반환되어야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe true
                responseBody["data"]["memberId"].asLong() shouldBe memberId
                responseBody["data"]["totalBalance"].asLong() shouldBe 1500L
                responseBody["data"]["availableBalance"].asLong() shouldBe 1500L
                responseBody["data"]["expiredBalance"].asLong() shouldBe 0L
                responseBody["data"]["accumulations"].isArray shouldBe true
                responseBody["data"]["accumulations"].size() shouldBe 2
                responseBody["message"].asText() shouldNotBe null
                
                verify { pointQueryUseCase.getBalance(memberId) }
            }
        }
    }
    
    Given("포인트 사용 내역 조회 API") {
        When("유효한 회원 ID로 사용 내역을 조회하면") {
            val memberId = 1L
            val page = 0
            val size = 20
            
            val usage = PointUsage(
                pointKey = "USAGE_KEY_001",
                memberId = memberId,
                orderNumber = OrderNumber.of("ORDER_001"),
                totalAmount = Money.of(1000L),
                createdAt = LocalDateTime.now()
            )
            
            val usagePage = PageImpl(
                listOf(usage),
                PageRequest.of(page, size),
                1L
            )
            
            every { 
                pointQueryUseCase.getUsageHistory(memberId, null, PageRequest.of(page, size)) 
            } returns usagePage
            
            val result = mockMvc.perform(
                get("/api/points/history/$memberId")
                    .param("page", page.toString())
                    .param("size", size.toString())
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            
            Then("사용 내역이 반환되어야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe true
                responseBody["data"]["content"].isArray shouldBe true
                responseBody["data"]["content"].size() shouldBe 1
                responseBody["data"]["page"].asInt() shouldBe page
                responseBody["data"]["size"].asInt() shouldBe size
                responseBody["data"]["totalElements"].asLong() shouldBe 1L
                responseBody["message"].asText() shouldNotBe null
                
                verify { pointQueryUseCase.getUsageHistory(memberId, null, PageRequest.of(page, size)) }
            }
        }
        
        When("주문번호로 필터링하여 조회하면") {
            val memberId = 1L
            val orderNumber = "ORDER_001"
            val page = 0
            val size = 20
            
            val usage = PointUsage(
                pointKey = "USAGE_KEY_001",
                memberId = memberId,
                orderNumber = OrderNumber.of(orderNumber),
                totalAmount = Money.of(1000L),
                createdAt = LocalDateTime.now()
            )
            
            val usagePage = PageImpl(
                listOf(usage),
                PageRequest.of(page, size),
                1L
            )
            
            every { 
                pointQueryUseCase.getUsageHistory(memberId, orderNumber, PageRequest.of(page, size)) 
            } returns usagePage
            
            val result = mockMvc.perform(
                get("/api/points/history/$memberId")
                    .param("orderNumber", orderNumber)
                    .param("page", page.toString())
                    .param("size", size.toString())
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            
            Then("필터링된 사용 내역이 반환되어야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe true
                responseBody["data"]["content"].isArray shouldBe true
                responseBody["data"]["content"].size() shouldBe 1
                responseBody["data"]["content"][0]["orderNumber"].asText() shouldBe orderNumber
                
                verify { pointQueryUseCase.getUsageHistory(memberId, orderNumber, PageRequest.of(page, size)) }
            }
        }
        
        When("페이징 파라미터 없이 조회하면") {
            val memberId = 1L
            
            val usagePage = PageImpl(
                emptyList<PointUsage>(),
                PageRequest.of(0, 20),
                0L
            )
            
            every { 
                pointQueryUseCase.getUsageHistory(memberId, null, PageRequest.of(0, 20)) 
            } returns usagePage
            
            val result = mockMvc.perform(
                get("/api/points/history/$memberId")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()
            
            Then("기본 페이징으로 조회되어야 한다") {
                val responseBody = objectMapper.readTree(result.response.contentAsString)
                responseBody["success"].asBoolean() shouldBe true
                responseBody["data"]["page"].asInt() shouldBe 0
                responseBody["data"]["size"].asInt() shouldBe 20
                
                verify { pointQueryUseCase.getUsageHistory(memberId, null, PageRequest.of(0, 20)) }
            }
        }
    }
})

