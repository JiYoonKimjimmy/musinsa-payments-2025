package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.config.fixtures.FakePointConfigPort
import com.musinsa.payments.point.domain.entity.PointConfig
import com.musinsa.payments.point.domain.exception.InvalidConfigKeyException
import com.musinsa.payments.point.domain.exception.InvalidConfigValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec

/**
 * PointConfigValidator 단위 테스트
 */
class PointConfigValidatorTest : BehaviorSpec({

    val pointConfigPort = FakePointConfigPort().apply { setupDefaultConfigs() }
    val validator = PointConfigValidator(pointConfigPort)

    beforeContainer {
        pointConfigPort.resetToDefaults()
    }
    
    Given("MAX_ACCUMULATION_AMOUNT_PER_TIME 설정 값 검증") {
        When("유효한 값이 주어지면") {
            Then("예외가 발생하지 않아야 한다") {
                validator.validateConfigValue("MAX_ACCUMULATION_AMOUNT_PER_TIME", "100000")
            }
        }
        
        When("범위를 벗어난 값이 주어지면") {
            Then("InvalidConfigValueException이 발생해야 한다") {
                shouldThrow<InvalidConfigValueException> {
                    validator.validateConfigValue("MAX_ACCUMULATION_AMOUNT_PER_TIME", "0")
                }
            }
        }
        
        When("숫자가 아닌 값이 주어지면") {
            Then("InvalidConfigValueException이 발생해야 한다") {
                shouldThrow<InvalidConfigValueException> {
                    validator.validateConfigValue("MAX_ACCUMULATION_AMOUNT_PER_TIME", "invalid")
                }
            }
        }
    }
    
    Given("MAX_BALANCE_PER_MEMBER 설정 값 검증") {
        When("유효한 값이 주어지면") {
            Then("예외가 발생하지 않아야 한다") {
                validator.validateConfigValue("MAX_BALANCE_PER_MEMBER", "10000000")
            }
        }
    }
    
    Given("DEFAULT_EXPIRATION_DAYS 설정 값 검증") {
        When("유효한 값이 주어지면") {
            Then("예외가 발생하지 않아야 한다") {
                validator.validateConfigValue("DEFAULT_EXPIRATION_DAYS", "365")
            }
        }
        
        When("범위를 벗어난 값이 주어지면") {
            Then("InvalidConfigValueException이 발생해야 한다") {
                shouldThrow<InvalidConfigValueException> {
                    validator.validateConfigValue("DEFAULT_EXPIRATION_DAYS", "2000")
                }
            }
        }
    }
    
    Given("유효하지 않은 설정 키 검증") {
        When("존재하지 않는 설정 키가 주어지면") {
            Then("InvalidConfigKeyException이 발생해야 한다") {
                shouldThrow<InvalidConfigKeyException> {
                    validator.validateConfigValue("INVALID_KEY", "100")
                }
            }
        }
    }
    
    Given("설정 간 의존성 검증") {
        When("유효한 의존성 관계일 때") {
            Then("예외가 발생하지 않아야 한다") {
                // FakePointConfigPort의 기본 설정 사용
                // MIN_EXPIRATION_DAYS: 1, MAX_EXPIRATION_DAYS: 1824, DEFAULT_EXPIRATION_DAYS: 365
                validator.validateConfigDependencies()
            }
        }

        When("최소 만료일이 최대 만료일보다 크거나 같을 때") {
            // 최소 만료일을 최대 만료일과 같게 설정
            val invalidMinDays = PointConfig("MIN_EXPIRATION_DAYS", "1824")
            pointConfigPort.save(invalidMinDays)

            Then("InvalidConfigValueException이 발생해야 한다") {
                shouldThrow<InvalidConfigValueException> {
                    validator.validateConfigDependencies()
                }
            }
        }

        When("기본 만료일이 범위를 벗어날 때") {
            // 기본 만료일을 최대값보다 크게 설정
            val invalidDefaultDays = PointConfig("DEFAULT_EXPIRATION_DAYS", "2000")
            pointConfigPort.save(invalidDefaultDays)

            Then("InvalidConfigValueException이 발생해야 한다") {
                shouldThrow<InvalidConfigValueException> {
                    validator.validateConfigDependencies()
                }
            }
        }
    }
})

