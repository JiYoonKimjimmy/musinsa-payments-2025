package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.fixtures.FakePointKeyGenerator
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointUsageDetailPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointUsageStatus
import com.musinsa.payments.point.domain.entity.fixtures.PointAccumulationFixture
import com.musinsa.payments.point.domain.exception.InsufficientPointException
import com.musinsa.payments.point.domain.service.PointUsagePriorityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

/**
 * PointUsageService 단위 테스트
 */
class PointUsageServiceTest : BehaviorSpec({
    
    val pointAccumulationPersistencePort = FakePointAccumulationPersistencePort()
    val pointUsagePersistencePort = FakePointUsagePersistencePort()
    val pointUsageDetailPersistencePort = FakePointUsageDetailPersistencePort()
    val pointKeyGenerator = FakePointKeyGenerator()
    val pointUsagePriorityService = PointUsagePriorityService()
    
    val service = PointUsageService(
        pointAccumulationPersistencePort,
        pointUsagePersistencePort,
        pointUsageDetailPersistencePort,
        pointKeyGenerator,
        pointUsagePriorityService
    )

    beforeContainer {
        // 각 Given 블록 전에 실행되어 테스트 간 격리 보장
        pointAccumulationPersistencePort.clear()
        pointUsageDetailPersistencePort.clear()
        pointUsagePersistencePort.clear()
        pointKeyGenerator.resetCounter()
    }

    Given("사용 가능한 포인트가 충분할 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val amount = 5000L

        val accumulation = PointAccumulationFixture.create(
            pointKey = "ACCUM01",
            memberId = memberId,
            amount = 10000L
        )

        val savedAccumulation = pointAccumulationPersistencePort.save(accumulation)
        val accumulationId = savedAccumulation.id
            ?: throw IllegalStateException("적립 건 ID가 없습니다.")

        val result = service.use(memberId, orderNumber, amount)

        Then("포인트 사용이 정상적으로 완료되어야 한다") {
            result.memberId shouldBe memberId
            result.orderNumber.value shouldBe orderNumber
            result.totalAmount.toLong() shouldBe amount
            result.status shouldBe PointUsageStatus.USED
            result.pointKey shouldBe "POINT-001"
            result.id shouldBe 1L

            val savedDetails = pointUsageDetailPersistencePort.findAll()
            savedDetails.size shouldBe 1

            savedDetails[0].apply {
                this.amount.toLong() shouldBe 5000L
                pointAccumulationId shouldBe accumulationId
                pointUsageId shouldBe result.id
            }

            val updatedAccumulation = pointAccumulationPersistencePort.findById(accumulationId).orElseThrow()
            updatedAccumulation.availableAmount.toLong() shouldBe (10000L - amount)
        }
    }
    
    Given("사용 가능한 잔액이 부족한 경우") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val amount = 10000L
        
        val accumulation = PointAccumulationFixture.create(
            pointKey = "ACCUM01",
            memberId = memberId,
            amount = 5000L
        )

        pointAccumulationPersistencePort.save(accumulation)
        
        Then("InsufficientPointException이 발생해야 한다") {
            shouldThrow<InsufficientPointException> {
                service.use(memberId, orderNumber, amount)
            }
        }
    }
    
    Given("여러 적립 건에서 포인트를 사용해야 할 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val amount = 15000L
        
        val accumulation1 = PointAccumulationFixture.create(
            pointKey = "ACCUM01",
            memberId = memberId,
            amount = 10000L,
            expirationDate = LocalDate.now().plusDays(365)
        )

        val accumulation2 = PointAccumulationFixture.create(
            pointKey = "ACCUM02",
            memberId = memberId,
            amount = 10000L,
            expirationDate = LocalDate.now().plusDays(200)
        )
        
        val savedAccumulation1 = pointAccumulationPersistencePort.save(accumulation1)
        val savedAccumulation2 = pointAccumulationPersistencePort.save(accumulation2)
        val accumulation1Id = savedAccumulation1.id
            ?: throw IllegalStateException("적립 건 ID가 없습니다.")
        val accumulation2Id = savedAccumulation2.id
            ?: throw IllegalStateException("적립 건 ID가 없습니다.")
        
        val result = service.use(memberId, orderNumber, amount)
        
        Then("여러 적립 건에서 포인트가 사용되어야 한다") {
            result.totalAmount.toLong() shouldBe amount
            
            val updatedAccumulation2 = pointAccumulationPersistencePort.findById(accumulation2Id).orElseThrow()
            val updatedAccumulation1 = pointAccumulationPersistencePort.findById(accumulation1Id).orElseThrow()

            updatedAccumulation2.availableAmount.toLong() shouldBe 0L
            updatedAccumulation1.availableAmount.toLong() shouldBe 5000L

            val savedDetails = pointUsageDetailPersistencePort.findAll()
            savedDetails.size shouldBe 2

            val detailFromAccumulation2 = savedDetails.find { it.pointAccumulationId == accumulation2Id }!!
            detailFromAccumulation2.apply {
                this.amount.toLong() shouldBe 10000L
                pointUsageId shouldBe result.id
            }

            val detailFromAccumulation1 = savedDetails.find { it.pointAccumulationId == accumulation1Id }!!
            detailFromAccumulation1.apply {
                this.amount.toLong() shouldBe 5000L
                pointUsageId shouldBe result.id
            }
        }
    }
    
    Given("적립 건별 집계 방식으로 1원 단위 정확도를 추적해야 할 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val amount = 3L

        val accumulation = PointAccumulationFixture.create(
            pointKey = "ACCUM01",
            memberId = memberId,
            amount = 10000L
        )

        val savedAccumulation = pointAccumulationPersistencePort.save(accumulation)
        val accumulationId = savedAccumulation.id
            ?: throw IllegalStateException("적립 건 ID가 없습니다.")

        val result = service.use(memberId, orderNumber, amount)

        Then("적립 건당 1개의 레코드로 1원 단위 정확도를 추적할 수 있어야 한다") {
            val savedDetails = pointUsageDetailPersistencePort.findAll()
            savedDetails.size shouldBe 1

            savedDetails[0].apply {
                this.amount.toLong() shouldBe 3L
                pointAccumulationId shouldBe accumulationId
                pointUsageId shouldBe result.id
            }
        }
    }
    
    Given("0원 이하의 금액 사용 요청이 있을 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val amount = 0L
        
        Then("IllegalArgumentException이 발생해야 한다") {
            shouldThrow<IllegalArgumentException> {
                service.use(memberId, orderNumber, amount)
            }
        }
    }
    
    Given("음수 금액 사용 요청이 있을 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val amount = -1000L
        
        Then("IllegalArgumentException이 발생해야 한다") {
            shouldThrow<IllegalArgumentException> {
                service.use(memberId, orderNumber, amount)
            }
        }
    }
})
