package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.config.fixtures.FakePointConfigHistoryPort
import com.musinsa.payments.point.application.port.output.config.fixtures.FakePointConfigPort
import com.musinsa.payments.point.domain.entity.PointConfig
import com.musinsa.payments.point.domain.exception.ConfigNotFoundException
import com.musinsa.payments.point.domain.exception.InvalidConfigValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * PointConfigService 단위 테스트
 */
class PointConfigServiceTest : BehaviorSpec({

    val pointConfigPort = FakePointConfigPort().apply { setupDefaultConfigs() }
    val pointConfigValidator = PointConfigValidator(pointConfigPort)
    val pointConfigHistoryPort = FakePointConfigHistoryPort()

    val service = PointConfigService(pointConfigPort, pointConfigValidator, pointConfigHistoryPort)

    beforeContainer {
        pointConfigPort.resetToDefaults()
        pointConfigHistoryPort.clear()
    }
    
    Given("존재하는 설정 키가 있을 때") {
        val configKey = "MAX_ACCUMULATION_AMOUNT_PER_TIME"

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

        When("설정을 조회하면") {
            val result = service.findByConfigKey(configKey)

            Then("empty를 반환해야 한다") {
                result.isEmpty shouldBe true
            }
        }
    }
    
    Given("여러 설정이 있을 때") {
        When("모든 설정을 조회하면") {
            val result = service.findAll()

            Then("모든 설정이 조회되어야 한다") {
                result.size shouldBe 5 // FakePointConfigPort의 기본 설정 개수
                // 주요 설정들이 포함되어 있는지 확인
                result.any { it.configKey == "MAX_ACCUMULATION_AMOUNT_PER_TIME" } shouldBe true
                result.any { it.configKey == "MAX_BALANCE_PER_MEMBER" } shouldBe true
                result.any { it.configKey == "DEFAULT_EXPIRATION_DAYS" } shouldBe true
            }
        }
    }
    
    Given("Long 타입 설정 값이 있을 때") {
        val configKey = "MAX_ACCUMULATION_AMOUNT_PER_TIME"

        When("Long 값으로 조회하면") {
            val result = service.getLongValue(configKey)

            Then("Long 타입으로 변환되어야 한다") {
                result shouldBe 100000L
            }
        }
    }

    Given("Int 타입 설정 값이 있을 때") {
        val configKey = "DEFAULT_EXPIRATION_DAYS"

        When("Int 값으로 조회하면") {
            val result = service.getIntValue(configKey)

            Then("Int 타입으로 변환되어야 한다") {
                result shouldBe 365
            }
        }
    }

    Given("Boolean 타입 설정 값이 있을 때") {
        val configKey = "FEATURE_ENABLED"

        When("Boolean 값으로 조회하면") {
            // Boolean 타입 설정을 추가
            val config = PointConfig(configKey, "true")
            pointConfigPort.save(config)

            val result = service.getBooleanValue(configKey)

            Then("Boolean 타입으로 변환되어야 한다") {
                result shouldBe true
            }
        }
    }
    
    Given("존재하지 않는 설정 키로 Long 값을 조회할 때") {
        val configKey = "NOT_FOUND"

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

        When("Long 값으로 조회하면") {
            // 숫자가 아닌 설정 값을 저장
            val config = PointConfig(configKey, "not_a_number")
            pointConfigPort.save(config)

            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.getLongValue(configKey)
                }
            }
        }

        When("Int 값으로 조회하면") {
            // 숫자가 아닌 설정 값을 저장
            val config = PointConfig(configKey, "not_a_number")
            pointConfigPort.save(config)

            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.getIntValue(configKey)
                }
            }
        }
    }
    
    Given("설정 업데이트") {
        val configKey = "MAX_ACCUMULATION_AMOUNT_PER_TIME"

        When("유효한 설정 값으로 업데이트하면") {
            val result = service.updateConfig(configKey, "200000")

            Then("설정이 업데이트되어야 한다") {
                result.configValue shouldBe "200000"

                // 변경 이력이 저장되었는지 확인
                val histories = pointConfigHistoryPort.findByConfigKey(configKey)
                histories.size shouldBe 1
            }
        }

        When("존재하지 않는 설정 키로 업데이트하면") {
            val nonExistentKey = "NON_EXISTENT_KEY"

            Then("ConfigNotFoundException이 발생해야 한다") {
                shouldThrow<ConfigNotFoundException> {
                    service.updateConfig(nonExistentKey, "200000")
                }
            }
        }

        When("유효하지 않은 설정 값으로 업데이트하면") {
            Then("InvalidConfigValueException이 발생해야 한다") {
                shouldThrow<InvalidConfigValueException> {
                    service.updateConfig(configKey, "invalid")
                }
            }
        }
    }
})
