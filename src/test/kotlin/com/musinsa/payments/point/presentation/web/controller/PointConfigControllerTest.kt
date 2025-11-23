package com.musinsa.payments.point.presentation.web.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.musinsa.payments.point.application.service.PointConfigService
import com.musinsa.payments.point.domain.entity.PointConfig
import com.musinsa.payments.point.domain.exception.ConfigNotFoundException
import com.musinsa.payments.point.presentation.web.exception.GlobalExceptionHandler
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.time.LocalDateTime
import java.util.*

/**
 * PointConfigController REST API 테스트
 */
class PointConfigControllerTest : BehaviorSpec({
    
    val pointConfigService = mockk<PointConfigService>()
    val controller = PointConfigController(pointConfigService)
    val exceptionHandler = GlobalExceptionHandler()
    val validator = LocalValidatorFactoryBean().apply {
        afterPropertiesSet()
    }
    val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(exceptionHandler)
        .setValidator(validator)
        .build()
    val objectMapper = ObjectMapper()
    
    Given("모든 설정 조회 API") {
        When("유효한 요청으로 조회하면") {
            val configs = listOf(
                PointConfig("MAX_ACCUMULATION_AMOUNT_PER_TIME", "100000", "1회 최대 적립 금액"),
                PointConfig("MAX_BALANCE_PER_MEMBER", "10000000", "개인별 최대 보유 금액")
            )
            
            every { pointConfigService.findAll() } returns configs
            
            val result = mockMvc.perform(get("/api/admin/points/config"))
            
            Then("200 OK와 설정 목록이 반환되어야 한다") {
                result.andExpect(status().isOk)
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray)
                    .andExpect(jsonPath("$.data[0].configKey").value("MAX_ACCUMULATION_AMOUNT_PER_TIME"))
            }
        }
    }
    
    Given("특정 설정 조회 API") {
        When("존재하는 설정 키로 조회하면") {
            val config = PointConfig("MAX_ACCUMULATION_AMOUNT_PER_TIME", "100000", "1회 최대 적립 금액")
            
            every { pointConfigService.findByConfigKey("MAX_ACCUMULATION_AMOUNT_PER_TIME") } returns Optional.of(config)
            
            val result = mockMvc.perform(get("/api/admin/points/config/MAX_ACCUMULATION_AMOUNT_PER_TIME"))
            
            Then("200 OK와 설정 정보가 반환되어야 한다") {
                result.andExpect(status().isOk)
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.configKey").value("MAX_ACCUMULATION_AMOUNT_PER_TIME"))
                    .andExpect(jsonPath("$.data.configValue").value("100000"))
            }
        }
        
        When("존재하지 않는 설정 키로 조회하면") {
            every { pointConfigService.findByConfigKey("NOT_FOUND") } returns Optional.empty()
            
            val result = mockMvc.perform(get("/api/admin/points/config/NOT_FOUND"))
            
            Then("404 Not Found가 반환되어야 한다") {
                result.andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("CONFIG_NOT_FOUND"))
            }
        }
    }
    
    Given("설정 업데이트 API") {
        When("유효한 요청으로 업데이트하면") {
            val config = PointConfig("MAX_ACCUMULATION_AMOUNT_PER_TIME", "200000", "1회 최대 적립 금액 (업데이트)")
            config.updatedAt = LocalDateTime.now()
            
            val request = mapOf(
                "configValue" to "200000",
                "description" to "1회 최대 적립 금액 (업데이트)"
            )
            
            every { pointConfigService.updateConfig("MAX_ACCUMULATION_AMOUNT_PER_TIME", "200000", "1회 최대 적립 금액 (업데이트)", null) } returns config
            
            val result = mockMvc.perform(
                put("/api/admin/points/config/MAX_ACCUMULATION_AMOUNT_PER_TIME")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            
            Then("200 OK와 업데이트된 설정이 반환되어야 한다") {
                result.andExpect(status().isOk)
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.configValue").value("200000"))
                
                verify { pointConfigService.updateConfig("MAX_ACCUMULATION_AMOUNT_PER_TIME", "200000", "1회 최대 적립 금액 (업데이트)", null) }
            }
        }
        
        When("유효하지 않은 요청으로 업데이트하면") {
            val request = mapOf(
                "configValue" to ""  // 빈 값
            )
            
            val result = mockMvc.perform(
                put("/api/admin/points/config/MAX_ACCUMULATION_AMOUNT_PER_TIME")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            
            Then("400 Bad Request가 반환되어야 한다") {
                result.andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.success").value(false))
            }
        }
        
        When("존재하지 않는 설정 키로 업데이트하면") {
            val request = mapOf(
                "configValue" to "200000"
            )
            
            every { 
                pointConfigService.updateConfig("NOT_FOUND", "200000", null, null) 
            } throws ConfigNotFoundException("설정을 찾을 수 없습니다: NOT_FOUND")
            
            val result = mockMvc.perform(
                put("/api/admin/points/config/NOT_FOUND")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            
            Then("404 Not Found가 반환되어야 한다") {
                result.andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("CONFIG_NOT_FOUND"))
            }
        }
    }
})

