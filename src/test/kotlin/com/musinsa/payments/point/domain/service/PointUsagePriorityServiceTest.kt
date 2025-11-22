package com.musinsa.payments.point.domain.service

import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.exception.InsufficientPointException
import com.musinsa.payments.point.domain.valueobject.Money
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class PointUsagePriorityServiceTest : BehaviorSpec({
    
    val service = PointUsagePriorityService()
    
    Given("수기 지급 포인트와 일반 포인트가 있을 때") {
        val manualGrant = PointAccumulation(
            pointKey = "MANUAL001",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365),
            isManualGrant = true
        )
        
        val normal = PointAccumulation(
            pointKey = "NORMAL001",
            memberId = 1L,
            amount = Money.of(500L),
            expirationDate = LocalDate.now().plusDays(100),
            isManualGrant = false
        )
        
        val accumulations = listOf(normal, manualGrant)
        val usageAmount = Money.of(1200L)
        
        When("포인트 사용 우선순위를 선택하면") {
            val selected = service.selectAccumulationsForUsage(1L, usageAmount, accumulations)
            
            Then("수기 지급 포인트가 우선 선택되어야 한다") {
                selected shouldHaveSize 2
                selected[0].isManualGrant shouldBe true
                selected[0].pointKey shouldBe "MANUAL001"
            }
        }
    }
    
    Given("만료일이 다른 일반 포인트들이 있을 때") {
        val longExpiration = PointAccumulation(
            pointKey = "LONG001",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365),
            isManualGrant = false
        )
        
        val shortExpiration = PointAccumulation(
            pointKey = "SHORT001",
            memberId = 1L,
            amount = Money.of(500L),
            expirationDate = LocalDate.now().plusDays(100),
            isManualGrant = false
        )
        
        val accumulations = listOf(longExpiration, shortExpiration)
        val usageAmount = Money.of(1200L)
        
        When("포인트 사용 우선순위를 선택하면") {
            val selected = service.selectAccumulationsForUsage(1L, usageAmount, accumulations)
            
            Then("만료일이 짧은 포인트가 우선 선택되어야 한다") {
                selected shouldHaveSize 2
                selected[0].pointKey shouldBe "SHORT001"
            }
        }
    }
    
    Given("사용 가능한 포인트가 충분할 때") {
        val accumulation1 = PointAccumulation(
            pointKey = "ACC001",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        
        val accumulation2 = PointAccumulation(
            pointKey = "ACC002",
            memberId = 1L,
            amount = Money.of(500L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        
        val accumulations = listOf(accumulation1, accumulation2)
        val usageAmount = Money.of(1200L)
        
        When("포인트 사용을 선택하면") {
            val selected = service.selectAccumulationsForUsage(1L, usageAmount, accumulations)
            
            Then("충분한 적립 건이 선택되어야 한다") {
                selected shouldHaveSize 2
            }
        }
    }
    
    Given("사용 가능한 포인트가 부족할 때") {
        val accumulation = PointAccumulation(
            pointKey = "ACC001",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        
        val accumulations = listOf(accumulation)
        val usageAmount = Money.of(1500L)
        
        When("포인트 사용을 선택하면") {
            Then("예외가 발생해야 한다") {
                shouldThrow<InsufficientPointException> {
                    service.selectAccumulationsForUsage(1L, usageAmount, accumulations)
                }
            }
        }
    }
    
    Given("만료된 포인트가 있을 때") {
        // 만료일이 오늘 이전인 포인트는 생성할 수 없으므로,
        // 사용 가능 잔액이 0인 포인트로 대체하여 테스트
        // (실제 만료된 포인트는 isExpired()로 필터링됨)
        val expired = PointAccumulation(
            pointKey = "EXPIRED001",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now()
        )
        expired.use(Money.of(1000L))  // 모두 사용하여 사용 가능 잔액이 0이 되도록 함
        
        val valid = PointAccumulation(
            pointKey = "VALID001",
            memberId = 1L,
            amount = Money.of(500L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        
        val accumulations = listOf(expired, valid)
        val usageAmount = Money.of(500L)
        
        When("포인트 사용을 선택하면") {
            val selected = service.selectAccumulationsForUsage(1L, usageAmount, accumulations)
            
            Then("사용 가능한 포인트만 선택되어야 한다") {
                selected shouldHaveSize 1
                selected[0].pointKey shouldBe "VALID001"
            }
        }
    }
    
    Given("사용 가능 잔액이 없는 포인트가 있을 때") {
        val used = PointAccumulation(
            pointKey = "USED001",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        used.use(Money.of(1000L))
        
        val available = PointAccumulation(
            pointKey = "AVAILABLE001",
            memberId = 1L,
            amount = Money.of(500L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        
        val accumulations = listOf(used, available)
        val usageAmount = Money.of(500L)
        
        When("포인트 사용을 선택하면") {
            val selected = service.selectAccumulationsForUsage(1L, usageAmount, accumulations)
            
            Then("사용 가능한 포인트만 선택되어야 한다") {
                selected shouldHaveSize 1
                selected[0].pointKey shouldBe "AVAILABLE001"
            }
        }
    }
    
    Given("수기 지급과 만료일이 다른 포인트들이 있을 때") {
        val manualLong = PointAccumulation(
            pointKey = "MANUAL_LONG",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365),
            isManualGrant = true
        )
        
        val manualShort = PointAccumulation(
            pointKey = "MANUAL_SHORT",
            memberId = 1L,
            amount = Money.of(500L),
            expirationDate = LocalDate.now().plusDays(100),
            isManualGrant = true
        )
        
        val normalShort = PointAccumulation(
            pointKey = "NORMAL_SHORT",
            memberId = 1L,
            amount = Money.of(500L),
            expirationDate = LocalDate.now().plusDays(50),
            isManualGrant = false
        )
        
        val accumulations = listOf(manualLong, normalShort, manualShort)
        val usageAmount = Money.of(1500L)
        
        When("포인트 사용을 선택하면") {
            val selected = service.selectAccumulationsForUsage(1L, usageAmount, accumulations)
            
            Then("수기 지급 포인트가 우선되고, 그 중 만료일이 짧은 것이 우선되어야 한다") {
                selected shouldHaveSize 2
                selected[0].pointKey shouldBe "MANUAL_SHORT"
                selected[1].pointKey shouldBe "MANUAL_LONG"
            }
        }
    }
})

