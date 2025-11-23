package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.PointKeyGenerator
import com.musinsa.payments.point.application.port.output.config.PointConfigPort
import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsageDetailPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.entity.PointUsageDetail
import com.musinsa.payments.point.domain.entity.PointUsageStatus
import com.musinsa.payments.point.domain.entity.PointConfig
import com.musinsa.payments.point.domain.exception.CannotCancelUsageException
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import com.musinsa.payments.point.domain.valueobject.PointKey
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.*

/**
 * PointCancellationService 단위 테스트
 */
class PointCancellationServiceTest : BehaviorSpec({
    
    val pointUsagePersistencePort = mockk<PointUsagePersistencePort>()
    val pointUsageDetailPersistencePort = mockk<PointUsageDetailPersistencePort>()
    val pointAccumulationPersistencePort = mockk<PointAccumulationPersistencePort>()
    val pointKeyGenerator = mockk<PointKeyGenerator>()
    val pointConfigPort = mockk<PointConfigPort>()
    
    val service = PointCancellationService(
        pointUsagePersistencePort,
        pointUsageDetailPersistencePort,
        pointAccumulationPersistencePort,
        pointKeyGenerator,
        pointConfigPort
    )
    
    Given("취소 가능한 포인트 사용 건이 있을 때") {
        val pointKey = "USAGE01"
        val memberId = 1L
        val orderNumber = "ORDER123"
        val totalAmount = Money.of(5000L)
        
        val usage = PointUsage(
            pointKey = pointKey,
            memberId = memberId,
            orderNumber = OrderNumber.of(orderNumber),
            totalAmount = totalAmount
        )
        usage.id = 1L
        
        val accumulation = PointAccumulation(
            pointKey = "ACCUM01",
            memberId = memberId,
            amount = Money.of(10000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.id = 1L
        accumulation.use(totalAmount) // 사용 처리
        
        val usageDetail = PointUsageDetail(
            pointUsageId = 1L,
            pointAccumulationId = 1L,
            amount = totalAmount
        )
        
        every { pointUsagePersistencePort.findByPointKey(pointKey) } returns Optional.of(usage)
        every { pointUsageDetailPersistencePort.findByUsagePointKey(pointKey) } returns listOf(usageDetail)
        every { pointAccumulationPersistencePort.findById(1L) } returns Optional.of(accumulation)
        every { pointUsagePersistencePort.save(any()) } answers { firstArg() }
        every { pointUsageDetailPersistencePort.saveAll(any()) } answers { firstArg() }
        every { pointAccumulationPersistencePort.save(any()) } answers { firstArg() }
        
        When("전체 취소하면") {
            val result = service.cancelUsage(pointKey)
            
            Then("사용 건이 FULLY_CANCELLED 상태로 변경되어야 한다") {
                result.status shouldBe PointUsageStatus.FULLY_CANCELLED
                result.cancelledAmount shouldBe totalAmount
                verify { pointAccumulationPersistencePort.save(any()) }
                verify { pointUsageDetailPersistencePort.saveAll(any()) }
            }
        }
    }
    
    Given("부분 취소 가능한 포인트 사용 건이 있을 때") {
        val pointKey = "USAGE02"
        val memberId = 1L
        val orderNumber = "ORDER123"
        val totalAmount = Money.of(10000L)
        val cancelAmount = 3000L
        
        val usage = PointUsage(
            pointKey = pointKey,
            memberId = memberId,
            orderNumber = OrderNumber.of(orderNumber),
            totalAmount = totalAmount
        )
        usage.id = 1L
        
        val accumulation = PointAccumulation(
            pointKey = "ACCUM01",
            memberId = memberId,
            amount = Money.of(20000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.id = 1L
        accumulation.use(totalAmount) // 사용 처리
        
        val usageDetail = PointUsageDetail(
            pointUsageId = 1L,
            pointAccumulationId = 1L,
            amount = totalAmount
        )
        
        every { pointUsagePersistencePort.findByPointKey(pointKey) } returns Optional.of(usage)
        every { pointUsageDetailPersistencePort.findByUsagePointKey(pointKey) } returns listOf(usageDetail)
        every { pointAccumulationPersistencePort.findById(1L) } returns Optional.of(accumulation)
        every { pointUsagePersistencePort.save(any()) } answers { firstArg() }
        every { pointUsageDetailPersistencePort.saveAll(any()) } answers { firstArg() }
        every { pointAccumulationPersistencePort.save(any()) } answers { firstArg() }
        
        When("부분 취소하면") {
            val result = service.cancelUsage(pointKey, cancelAmount)
            
            Then("사용 건이 PARTIALLY_CANCELLED 상태로 변경되어야 한다") {
                result.status shouldBe PointUsageStatus.PARTIALLY_CANCELLED
                result.cancelledAmount.toLong() shouldBe cancelAmount
                verify { pointAccumulationPersistencePort.save(any()) }
            }
        }
    }
    
    Given("존재하지 않는 사용 건 키가 있을 때") {
        val pointKey = "NOTFOUND"
        
        every { pointUsagePersistencePort.findByPointKey(pointKey) } returns Optional.empty()
        
        When("취소하면") {
            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.cancelUsage(pointKey)
                }
            }
        }
    }
    
    Given("취소 불가능한 금액이 있을 때") {
        val pointKey = "USAGE03"
        val memberId = 1L
        val orderNumber = "ORDER123"
        val totalAmount = Money.of(5000L)
        val cancelAmount = 10000L // 총액보다 큰 금액
        
        val usage = PointUsage(
            pointKey = pointKey,
            memberId = memberId,
            orderNumber = OrderNumber.of(orderNumber),
            totalAmount = totalAmount
        )
        usage.id = 1L
        
        every { pointUsagePersistencePort.findByPointKey(pointKey) } returns Optional.of(usage)
        
        When("취소하면") {
            Then("CannotCancelUsageException이 발생해야 한다") {
                shouldThrow<CannotCancelUsageException> {
                    service.cancelUsage(pointKey, cancelAmount)
                }
            }
        }
    }
    
    Given("만료된 포인트가 포함된 사용 건이 있을 때") {
        val pointKey = "USAGE04"
        val memberId = 1L
        val orderNumber = "ORDER123"
        val totalAmount = Money.of(5000L)
        val newPointKey = PointKey.of("NEWACCUM")
        
        val usage = PointUsage(
            pointKey = pointKey,
            memberId = memberId,
            orderNumber = OrderNumber.of(orderNumber),
            totalAmount = totalAmount
        )
        usage.id = 1L
        
        // 만료된 적립 건 (생성자는 만료일 검증을 하므로, 일단 유효한 날짜로 생성 후 변경)
        val expiredAccumulation = PointAccumulation(
            pointKey = "EXPIRED01",
            memberId = memberId,
            amount = Money.of(10000L),
            expirationDate = LocalDate.now().plusDays(1) // 일단 유효한 날짜로 생성
        )
        expiredAccumulation.id = 1L
        expiredAccumulation.expirationDate = LocalDate.now().minusDays(1) // 만료일을 과거로 변경
        expiredAccumulation.use(totalAmount) // 사용 처리
        
        val usageDetail = PointUsageDetail(
            pointUsageId = 1L,
            pointAccumulationId = 1L,
            amount = totalAmount
        )
        
        every { pointUsagePersistencePort.findByPointKey(pointKey) } returns Optional.of(usage)
        every { pointUsageDetailPersistencePort.findByUsagePointKey(pointKey) } returns listOf(usageDetail)
        every { pointAccumulationPersistencePort.findById(1L) } returns Optional.of(expiredAccumulation)
        every { pointConfigPort.findByConfigKey("DEFAULT_EXPIRATION_DAYS") } returns Optional.of(
            PointConfig("DEFAULT_EXPIRATION_DAYS", "365")
        )
        every { pointKeyGenerator.generate() } returns newPointKey
        every { pointUsagePersistencePort.save(any()) } answers { firstArg() }
        every { pointUsageDetailPersistencePort.saveAll(any()) } answers { firstArg() }
        every { pointAccumulationPersistencePort.save(any()) } answers { firstArg() }
        
        When("취소하면") {
            val result = service.cancelUsage(pointKey)
            
            Then("만료된 포인트는 신규 적립으로 처리되어야 한다") {
                result.status shouldBe PointUsageStatus.FULLY_CANCELLED
                verify { pointAccumulationPersistencePort.save(any()) }
            }
        }
    }
    
    Given("만료되지 않은 포인트가 포함된 사용 건이 있을 때") {
        val pointKey = "USAGE05"
        val memberId = 1L
        val orderNumber = "ORDER123"
        val totalAmount = Money.of(5000L)
        
        val usage = PointUsage(
            pointKey = pointKey,
            memberId = memberId,
            orderNumber = OrderNumber.of(orderNumber),
            totalAmount = totalAmount
        )
        usage.id = 1L
        
        // 만료되지 않은 적립 건
        val accumulation = PointAccumulation(
            pointKey = "ACCUM01",
            memberId = memberId,
            amount = Money.of(10000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.id = 1L
        accumulation.use(totalAmount) // 사용 처리
        
        val usageDetail = PointUsageDetail(
            pointUsageId = 1L,
            pointAccumulationId = 1L,
            amount = totalAmount
        )
        
        every { pointUsagePersistencePort.findByPointKey(pointKey) } returns Optional.of(usage)
        every { pointUsageDetailPersistencePort.findByUsagePointKey(pointKey) } returns listOf(usageDetail)
        every { pointAccumulationPersistencePort.findById(1L) } returns Optional.of(accumulation)
        every { pointUsagePersistencePort.save(any()) } answers { firstArg() }
        every { pointUsageDetailPersistencePort.saveAll(any()) } answers { firstArg() }
        every { pointAccumulationPersistencePort.save(any()) } answers { firstArg() }
        
        When("취소하면") {
            service.cancelUsage(pointKey)
            
            Then("기존 적립 건이 복원되어야 한다") {
                verify { pointAccumulationPersistencePort.save(any()) }
            }
        }
    }
})
