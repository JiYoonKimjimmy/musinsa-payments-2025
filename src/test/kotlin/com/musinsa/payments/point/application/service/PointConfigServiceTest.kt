package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.config.PointConfigPort
import com.musinsa.payments.point.domain.entity.PointConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.*

/**
 * PointConfigService 단위 테스트
 */
class PointConfigServiceTest : BehaviorSpec({
    
    val pointConfigPort = mockk<PointConfigPort>()
    
    val service = PointConfigService(pointConfigPort)
    
    Given("존재하는 설정 키가 있을 때") {
        val configKey = "MAX_ACCUMULATION_AMOUNT_PER_TIME"
        val config = PointConfig(configKey, "100000")
        
        every { pointConfigPort.findByConfigKey(configKey) } returns Optional.of(config)
        
        When("설정을 조회하면") {
            val result = service.findByConfigKey(configKey)
            
            Then("설정이 정상적으로 조회되어야 한다") {
                result.isPresent shouldBe true
                result.get().configKey shouldBe configKey
                result.get().configValue shouldBe "100000"
            }
        }
    }
    
    Given("존재하지 않는 설정 키가 있을 때") {
        val configKey = "NOT_FOUND"
        
        every { pointConfigPort.findByConfigKey(configKey) } returns Optional.empty()
        
        When("설정을 조회하면") {
            val result = service.findByConfigKey(configKey)
            
            Then("empty를 반환해야 한다") {
                result.isEmpty shouldBe true
            }
        }
    }
    
    Given("여러 설정이 있을 때") {
        val configs = listOf(
            PointConfig("MAX_ACCUMULATION_AMOUNT_PER_TIME", "100000"),
            PointConfig("MAX_BALANCE_PER_MEMBER", "10000000"),
            PointConfig("DEFAULT_EXPIRATION_DAYS", "365")
        )
        
        every { pointConfigPort.findAll() } returns configs
        
        When("모든 설정을 조회하면") {
            val result = service.findAll()
            
            Then("모든 설정이 조회되어야 한다") {
                result.size shouldBe 3
                result[0].configKey shouldBe "MAX_ACCUMULATION_AMOUNT_PER_TIME"
                result[1].configKey shouldBe "MAX_BALANCE_PER_MEMBER"
                result[2].configKey shouldBe "DEFAULT_EXPIRATION_DAYS"
            }
        }
    }
    
    Given("Long 타입 설정 값이 있을 때") {
        val configKey = "MAX_ACCUMULATION_AMOUNT_PER_TIME"
        val config = PointConfig(configKey, "100000")
        
        every { pointConfigPort.findByConfigKey(configKey) } returns Optional.of(config)
        
        When("Long 값으로 조회하면") {
            val result = service.getLongValue(configKey)
            
            Then("Long 타입으로 변환되어야 한다") {
                result shouldBe 100000L
            }
        }
    }
    
    Given("Int 타입 설정 값이 있을 때") {
        val configKey = "DEFAULT_EXPIRATION_DAYS"
        val config = PointConfig(configKey, "365")
        
        every { pointConfigPort.findByConfigKey(configKey) } returns Optional.of(config)
        
        When("Int 값으로 조회하면") {
            val result = service.getIntValue(configKey)
            
            Then("Int 타입으로 변환되어야 한다") {
                result shouldBe 365
            }
        }
    }
    
    Given("Boolean 타입 설정 값이 있을 때") {
        val configKey = "FEATURE_ENABLED"
        val config = PointConfig(configKey, "true")
        
        every { pointConfigPort.findByConfigKey(configKey) } returns Optional.of(config)
        
        When("Boolean 값으로 조회하면") {
            val result = service.getBooleanValue(configKey)
            
            Then("Boolean 타입으로 변환되어야 한다") {
                result shouldBe true
            }
        }
    }
    
    Given("존재하지 않는 설정 키로 Long 값을 조회할 때") {
        val configKey = "NOT_FOUND"
        
        every { pointConfigPort.findByConfigKey(configKey) } returns Optional.empty()
        
        When("Long 값으로 조회하면") {
            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.getLongValue(configKey)
                }
            }
        }
    }
    
    Given("존재하지 않는 설정 키로 Int 값을 조회할 때") {
        val configKey = "NOT_FOUND"
        
        every { pointConfigPort.findByConfigKey(configKey) } returns Optional.empty()
        
        When("Int 값으로 조회하면") {
            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.getIntValue(configKey)
                }
            }
        }
    }
    
    Given("존재하지 않는 설정 키로 Boolean 값을 조회할 때") {
        val configKey = "NOT_FOUND"
        
        every { pointConfigPort.findByConfigKey(configKey) } returns Optional.empty()
        
        When("Boolean 값으로 조회하면") {
            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.getBooleanValue(configKey)
                }
            }
        }
    }
    
    Given("숫자가 아닌 설정 값이 있을 때") {
        val configKey = "INVALID_CONFIG"
        val config = PointConfig(configKey, "not_a_number")
        
        every { pointConfigPort.findByConfigKey(configKey) } returns Optional.of(config)
        
        When("Long 값으로 조회하면") {
            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.getLongValue(configKey)
                }
            }
        }
        
        When("Int 값으로 조회하면") {
            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.getIntValue(configKey)
                }
            }
        }
    }
})
