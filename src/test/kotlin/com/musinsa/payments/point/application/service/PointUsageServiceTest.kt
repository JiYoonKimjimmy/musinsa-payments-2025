package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.PointKeyGenerator
import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsageDetailPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.entity.PointUsageStatus
import com.musinsa.payments.point.domain.exception.InsufficientPointException
import com.musinsa.payments.point.domain.service.PointUsagePriorityService
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.PointKey
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

/**
 * PointUsageService 단위 테스트
 */
class PointUsageServiceTest : BehaviorSpec({
    
    val pointAccumulationPersistencePort = mockk<PointAccumulationPersistencePort>()
    val pointUsagePersistencePort = mockk<PointUsagePersistencePort>()
    val pointUsageDetailPersistencePort = mockk<PointUsageDetailPersistencePort>()
    val pointKeyGenerator = mockk<PointKeyGenerator>()
    val pointUsagePriorityService = mockk<PointUsagePriorityService>()
    
    val service = PointUsageService(
        pointAccumulationPersistencePort,
        pointUsagePersistencePort,
        pointUsageDetailPersistencePort,
        pointKeyGenerator,
        pointUsagePriorityService
    )
    
    fun createSelectedAccumulation(
        pointKey: String,
        memberId: Long,
        amount: Long,
        id: Long
    ): PointAccumulation {
        val accumulation = PointAccumulation(
            pointKey = pointKey,
            memberId = memberId,
            amount = Money.of(amount),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.id = id
        accumulation.availableAmount = Money.of(amount)
        return accumulation
    }
    
    Given("사용 가능한 포인트가 충분할 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val amount = 5000L
        val pointKey = PointKey.of("USAGE01")
        
        val accumulation = PointAccumulation(
            pointKey = "ACCUM01",
            memberId = memberId,
            amount = Money.of(10000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.id = 1L
        
        val selectedAccumulation = createSelectedAccumulation("ACCUM01", memberId, 10000L, 1L)
        
        every { pointAccumulationPersistencePort.sumAvailableAmountByMemberId(memberId) } returns Money.of(10000L)
        every { pointAccumulationPersistencePort.findAvailableAccumulationsByMemberId(memberId) } returns listOf(accumulation)
        every { pointUsagePriorityService.selectAccumulationsForUsage(memberId, Money.of(amount), any()) } returns listOf(selectedAccumulation)
        every { pointKeyGenerator.generate() } returns pointKey
        every { pointAccumulationPersistencePort.save(any()) } answers {
            val acc = firstArg<PointAccumulation>()
            if (acc.id == null) {
                acc.id = 1L
            }
            acc
        }
        every { pointUsagePersistencePort.save(any()) } answers {
            val usage = firstArg<PointUsage>()
            usage.id = 1L
            usage
        }
        every { pointUsageDetailPersistencePort.saveAll(any()) } answers { firstArg() }
        
        When("포인트를 사용하면") {
            val result = service.use(memberId, orderNumber, amount)
            
            Then("포인트 사용이 정상적으로 완료되어야 한다") {
                result.memberId shouldBe memberId
                result.orderNumber.value shouldBe orderNumber
                result.totalAmount.toLong() shouldBe amount
                result.status shouldBe PointUsageStatus.USED
                result.pointKey shouldBe pointKey.value
                
                verify { pointUsageDetailPersistencePort.saveAll(any()) }
                verify(exactly = 1) { pointAccumulationPersistencePort.save(any()) }
                verify(exactly = 1) { pointUsagePersistencePort.save(any()) }
            }
        }
    }
    
    Given("사용 가능한 잔액이 부족할 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val amount = 10000L
        
        every { pointAccumulationPersistencePort.sumAvailableAmountByMemberId(memberId) } returns Money.of(5000L)
        
        When("포인트를 사용하면") {
            Then("InsufficientPointException이 발생해야 한다") {
                shouldThrow<InsufficientPointException> {
                    service.use(memberId, orderNumber, amount)
                }
            }
        }
    }
    
    Given("여러 적립 건에서 포인트를 사용해야 할 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val amount = 15000L // 두 적립 건에서 사용
        val pointKey = PointKey.of("USAGE02")
        
        val accumulation1 = PointAccumulation(
            pointKey = "ACCUM01",
            memberId = memberId,
            amount = Money.of(10000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation1.id = 1L
        
        val accumulation2 = PointAccumulation(
            pointKey = "ACCUM02",
            memberId = memberId,
            amount = Money.of(10000L),
            expirationDate = LocalDate.now().plusDays(200)
        )
        accumulation2.id = 2L
        
        val selectedAccumulation1 = createSelectedAccumulation("ACCUM01", memberId, 10000L, 1L)
        val selectedAccumulation2 = createSelectedAccumulation("ACCUM02", memberId, 10000L, 2L)
        
        every { pointAccumulationPersistencePort.sumAvailableAmountByMemberId(memberId) } returns Money.of(20000L)
        every { pointAccumulationPersistencePort.findAvailableAccumulationsByMemberId(memberId) } returns listOf(accumulation1, accumulation2)
        every { pointUsagePriorityService.selectAccumulationsForUsage(memberId, Money.of(amount), any()) } returns listOf(selectedAccumulation1, selectedAccumulation2)
        every { pointKeyGenerator.generate() } returns pointKey
        var saveCount = 0
        every { pointAccumulationPersistencePort.save(any()) } answers {
            val acc = firstArg<PointAccumulation>()
            if (acc.id == null) {
                saveCount++
                acc.id = saveCount.toLong()
            }
            acc
        }
        every { pointUsagePersistencePort.save(any()) } answers {
            val usage = firstArg<PointUsage>()
            usage.id = 1L
            usage
        }
        every { pointUsageDetailPersistencePort.saveAll(any()) } answers { firstArg() }
        
        When("포인트를 사용하면") {
            val result = service.use(memberId, orderNumber, amount)
            
            Then("여러 적립 건에서 포인트가 사용되어야 한다") {
                result.totalAmount.toLong() shouldBe amount
                verify(atLeast = 2) { pointAccumulationPersistencePort.save(any()) }
            }
        }
    }
    
    Given("1원 단위로 상세 내역을 생성해야 할 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val amount = 3L // 3원 사용
        val pointKey = PointKey.of("USAGE03")
        
        val accumulation = PointAccumulation(
            pointKey = "ACCUM01",
            memberId = memberId,
            amount = Money.of(10000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.id = 1L
        
        val selectedAccumulation = createSelectedAccumulation("ACCUM01", memberId, 10000L, 1L)
        
        var savedDetails: List<com.musinsa.payments.point.domain.entity.PointUsageDetail> = emptyList()
        
        every { pointAccumulationPersistencePort.sumAvailableAmountByMemberId(memberId) } returns Money.of(10000L)
        every { pointAccumulationPersistencePort.findAvailableAccumulationsByMemberId(memberId) } returns listOf(accumulation)
        every { pointUsagePriorityService.selectAccumulationsForUsage(memberId, Money.of(amount), any()) } returns listOf(selectedAccumulation)
        every { pointKeyGenerator.generate() } returns pointKey
        every { pointAccumulationPersistencePort.save(any()) } answers {
            val acc = firstArg<PointAccumulation>()
            if (acc.id == null) {
                acc.id = 1L
            }
            acc
        }
        every { pointUsagePersistencePort.save(any()) } answers {
            val usage = firstArg<PointUsage>()
            usage.id = 1L
            usage
        }
        every { pointUsageDetailPersistencePort.saveAll(any()) } answers {
            savedDetails = firstArg()
            savedDetails
        }
        
        When("포인트를 사용하면") {
            service.use(memberId, orderNumber, amount)
            
            Then("1원 단위로 상세 내역이 생성되어야 한다") {
                savedDetails.size shouldBe 3 // 3원이므로 3개의 상세 내역
                savedDetails.forEach { detail ->
                    detail.amount.toLong() shouldBe 1L // 각 상세 내역은 1원
                    detail.pointAccumulationId shouldBe 1L
                }
            }
        }
    }
    
    Given("0원 이하의 금액 사용 요청이 있을 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val amount = 0L
        
        When("포인트를 사용하면") {
            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.use(memberId, orderNumber, amount)
                }
            }
        }
    }
    
    Given("음수 금액 사용 요청이 있을 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val amount = -1000L
        
        When("포인트를 사용하면") {
            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.use(memberId, orderNumber, amount)
                }
            }
        }
    }
})
