package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.config.fixtures.FakePointConfigPort
import com.musinsa.payments.point.application.port.output.fixtures.FakePointKeyGenerator
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointAccumulationPersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.domain.exception.InvalidExpirationDateException
import com.musinsa.payments.point.domain.exception.MaxAccumulationExceededException
import com.musinsa.payments.point.domain.exception.MaxBalanceExceededException
import com.musinsa.payments.point.domain.valueobject.Money
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

/**
 * PointAccumulationService 단위 테스트
 */
class PointAccumulationServiceTest : BehaviorSpec({
    
    val pointAccumulationPersistencePort = FakePointAccumulationPersistencePort()
    val pointConfigPort = FakePointConfigPort().apply { setupDefaultConfigs() }
    val pointKeyGenerator = FakePointKeyGenerator()
    val service = PointAccumulationService(
        pointAccumulationPersistencePort,
        pointConfigPort,
        pointKeyGenerator
    )

    beforeSpec {
        pointAccumulationPersistencePort.clear()
        pointKeyGenerator.resetCounter()
    }
    
    Given("유효한 적립 요청이 있을 때") {
        val memberId = 1L
        val amount = 10000L
        
        When("포인트를 적립하면") {
            val result = service.accumulate(memberId, amount)
            
            Then("적립이 정상적으로 완료되어야 한다") {
                result.memberId shouldBe memberId
                result.amount.toLong() shouldBe amount
                result.pointKey shouldBe "POINT-001"
                result.status shouldBe PointAccumulationStatus.ACCUMULATED
                result.isManualGrant shouldBe false
                result.id shouldBe 1L
            }
        }
    }
    
    Given("수기 지급 포인트 적립 요청이 있을 때") {
        val memberId = 1L
        val amount = 5000L
        
        When("수기 지급으로 포인트를 적립하면") {
            val result = service.accumulate(memberId, amount, isManualGrant = true)
            
            Then("수기 지급 플래그가 설정되어야 한다") {
                result.isManualGrant shouldBe true
            }
        }
    }
    
    Given("최대 적립 금액을 초과하는 적립 요청이 있을 때") {
        pointConfigPort.setConfig("MAX_ACCUMULATION_AMOUNT_PER_TIME", "100000")
        
        val memberId = 1L
        val amount = 200000L // 최대값 100000 초과
        
        When("포인트를 적립하면") {
            Then("MaxAccumulationExceededException이 발생해야 한다") {
                shouldThrow<MaxAccumulationExceededException> {
                    service.accumulate(memberId, amount)
                }
            }
        }
    }
    
    Given("최대 보유 금액을 초과하는 적립 요청이 있을 때") {
        pointConfigPort.setConfigs(
            "MAX_ACCUMULATION_AMOUNT_PER_TIME" to "1000000",
            "MAX_BALANCE_PER_MEMBER" to "10000000",
            "DEFAULT_EXPIRATION_DAYS" to "365",
            "MIN_EXPIRATION_DAYS" to "1",
            "MAX_EXPIRATION_DAYS" to "1824"
        )
        
        val memberId = 1L
        val currentBalance = 9600000L // 현재 잔액
        val amount = 500000L // 적립 금액 (9600000 + 500000 = 10100000 > 10000000)
        
        val existingAccumulation = PointAccumulation(
            pointKey = "EXISTING",
            memberId = memberId,
            amount = Money.of(currentBalance),
            expirationDate = LocalDate.now().plusDays(365)
        )

        pointAccumulationPersistencePort.save(existingAccumulation)

        When("포인트를 적립하면") {
            Then("MaxBalanceExceededException이 발생해야 한다") {
                shouldThrow<MaxBalanceExceededException> {
                    service.accumulate(memberId, amount)
                }
            }
        }
    }
    
    Given("사용자 지정 만료일이 있는 적립 요청이 있을 때") {
        pointConfigPort.setConfigs(
            "MAX_ACCUMULATION_AMOUNT_PER_TIME" to "100000",
            "MAX_BALANCE_PER_MEMBER" to "10000000",
            "MIN_EXPIRATION_DAYS" to "1",
            "MAX_EXPIRATION_DAYS" to "1824"
        )
        
        val memberId = 1L
        val amount = 10000L
        val expirationDays = 180
        
        When("지정한 만료일로 포인트를 적립하면") {
            val result = service.accumulate(memberId, amount, expirationDays)
            
            Then("지정한 만료일이 설정되어야 한다") {
                val expectedExpirationDate = LocalDate.now().plusDays(expirationDays.toLong())
                result.expirationDate shouldBe expectedExpirationDate
            }
        }
    }
    
    Given("만료일이 최소값보다 작은 적립 요청이 있을 때") {
        pointConfigPort.setConfigs(
            "MAX_ACCUMULATION_AMOUNT_PER_TIME" to "100000",
            "MAX_BALANCE_PER_MEMBER" to "10000000",
            "MIN_EXPIRATION_DAYS" to "1",
            "MAX_EXPIRATION_DAYS" to "1824"
        )
        
        val memberId = 1L
        val amount = 10000L
        val expirationDays = 0 // 최소값 1보다 작음
        
        When("포인트를 적립하면") {
            Then("InvalidExpirationDateException이 발생해야 한다") {
                shouldThrow<InvalidExpirationDateException> {
                    service.accumulate(memberId, amount, expirationDays)
                }
            }
        }
    }
    
    Given("만료일이 최대값보다 큰 적립 요청이 있을 때") {
        pointConfigPort.setConfigs(
            "MAX_ACCUMULATION_AMOUNT_PER_TIME" to "100000",
            "MAX_BALANCE_PER_MEMBER" to "10000000",
            "MIN_EXPIRATION_DAYS" to "1",
            "MAX_EXPIRATION_DAYS" to "1824"
        )
        
        val memberId = 1L
        val amount = 10000L
        val expirationDays = 2000 // 최대값 1824보다 큼
        
        When("포인트를 적립하면") {
            Then("InvalidExpirationDateException이 발생해야 한다") {
                shouldThrow<InvalidExpirationDateException> {
                    service.accumulate(memberId, amount, expirationDays)
                }
            }
        }
    }
    
    Given("취소 가능한 적립 건이 있을 때") {
        val pointKey = "TEST1234"
        val memberId = 1L
        val amount = Money.of(10000L)
        val accumulation = PointAccumulation(
            pointKey = pointKey,
            memberId = memberId,
            amount = amount,
            expirationDate = LocalDate.now().plusDays(365)
        )
        
        pointAccumulationPersistencePort.save(accumulation)

        When("적립을 취소하면") {
            Then("적립 상태가 CANCELLED로 변경되어야 한다") {
                val result = service.cancelAccumulation(pointKey)
                result.status shouldBe PointAccumulationStatus.CANCELLED
            }
        }
    }
    
    Given("존재하지 않는 적립 건 키가 있을 때") {
        val pointKey = "NOTFOUND"
        
        When("적립을 취소하면") {
            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.cancelAccumulation(pointKey)
                }
            }
        }
    }
})
