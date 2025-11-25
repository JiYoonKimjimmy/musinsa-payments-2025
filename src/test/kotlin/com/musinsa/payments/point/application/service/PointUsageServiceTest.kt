package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.fixtures.FakePointKeyGenerator
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointUsageDetailPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.domain.entity.PointUsageStatus
import com.musinsa.payments.point.domain.exception.InsufficientPointException
import com.musinsa.payments.point.domain.service.PointUsagePriorityService
import com.musinsa.payments.point.domain.valueobject.Money
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
        // Given 블록 전에 실행되어 테스트 간 격리 보장
        pointAccumulationPersistencePort.clear()
        pointUsageDetailPersistencePort.clear()
    }
    
    beforeEach {
        pointUsagePersistencePort.clear()
        pointKeyGenerator.resetCounter()
    }

    Given("포인트 사용 요청되었을 때") {
        When("사용 가능한 포인트가 충분할 때") {
            val memberId = 1L
            val orderNumber = "ORDER123"
            val amount = 5000L

            // 데이터 준비
            val accumulation = PointAccumulation(
                pointKey = "ACCUM01",
                memberId = memberId,
                amount = Money.of(10000L),
                expirationDate = LocalDate.now().plusDays(365),
                status = PointAccumulationStatus.ACCUMULATED
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

                // 상세 내역이 저장되었는지 확인
                val savedDetails = pointUsageDetailPersistencePort.findAll()
                savedDetails.size shouldBe amount
                savedDetails.forEach { detail ->
                    detail.amount.toLong() shouldBe 1L
                    detail.pointAccumulationId shouldBe accumulationId
                    detail.pointUsageId shouldBe result.id
                }
                
                // 적립 건의 사용 가능 잔액이 차감되었는지 확인
                val updatedAccumulation = pointAccumulationPersistencePort.findById(accumulationId).orElseThrow()
                updatedAccumulation.availableAmount.toLong() shouldBe (10000L - amount)
            }
        }
        
        When("사용 가능한 잔액이 부족한 경우") {
            val memberId = 1L
            val orderNumber = "ORDER123"
            val amount = 10000L
            
            // 데이터 준비
            val accumulation = PointAccumulation(
                pointKey = "ACCUM01",
                memberId = memberId,
                amount = Money.of(5000L), // 사용 요청보다 적은 금액
                expirationDate = LocalDate.now().plusDays(365)
            )
            
            pointAccumulationPersistencePort.save(accumulation)
            
            Then("InsufficientPointException이 발생해야 한다") {
                shouldThrow<InsufficientPointException> {
                    service.use(memberId, orderNumber, amount)
                }
            }
        }
        
        When("여러 적립 건에서 포인트를 사용해야 할 때") {
            val memberId = 1L
            val orderNumber = "ORDER123"
            val amount = 15000L // 두 적립 건에서 사용
            
            // 데이터 준비
            val accumulation1 = PointAccumulation(
                pointKey = "ACCUM01",
                memberId = memberId,
                amount = Money.of(10000L),
                expirationDate = LocalDate.now().plusDays(365),
                status = PointAccumulationStatus.ACCUMULATED
            )
            
            val accumulation2 = PointAccumulation(
                pointKey = "ACCUM02",
                memberId = memberId,
                amount = Money.of(10000L),
                expirationDate = LocalDate.now().plusDays(200), // 만료일이 더 짧음 (우선순위)
                status = PointAccumulationStatus.ACCUMULATED
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
                
                // 두 적립 건이 모두 업데이트되었는지 확인
                val savedAccumulations = pointAccumulationPersistencePort.findAll()
                savedAccumulations.size shouldBe 2
                
                // 우선순위에 따라 accumulation2(만료일이 더 짧음)가 먼저 사용되어야 함
                val updatedAccumulation2 = pointAccumulationPersistencePort.findById(accumulation2Id).orElseThrow()
                val updatedAccumulation1 = pointAccumulationPersistencePort.findById(accumulation1Id).orElseThrow()
                
                // accumulation2는 10000원 모두 사용됨
                updatedAccumulation2.availableAmount.toLong() shouldBe 0L
                // accumulation1은 5000원 사용됨
                updatedAccumulation1.availableAmount.toLong() shouldBe 5000L
                
                // 상세 내역이 저장되었는지 확인
                val savedDetails = pointUsageDetailPersistencePort.findAll()
                savedDetails.size shouldBe amount
                
                // 상세 내역의 pointAccumulationId 검증
                val detailsFromAccumulation2 = savedDetails.filter { it.pointAccumulationId == accumulation2Id }
                val detailsFromAccumulation1 = savedDetails.filter { it.pointAccumulationId == accumulation1Id }
                
                detailsFromAccumulation2.size shouldBe 10000L // accumulation2에서 10000원 사용
                detailsFromAccumulation1.size shouldBe 5000L // accumulation1에서 5000원 사용
                
                // 모든 상세 내역이 올바른 pointUsageId를 가지고 있는지 확인
                savedDetails.forEach { detail ->
                    detail.pointUsageId shouldBe result.id
                    detail.amount.toLong() shouldBe 1L
                }
            }
        }
        
        When("1원 단위로 상세 내역을 생성해야 할 때") {
            val memberId = 1L
            val orderNumber = "ORDER123"
            val amount = 3L // 3원 사용
            
            // 데이터 준비
            val accumulation = PointAccumulation(
                pointKey = "ACCUM01",
                memberId = memberId,
                amount = Money.of(10000L),
                expirationDate = LocalDate.now().plusDays(365),
                status = PointAccumulationStatus.ACCUMULATED
            )
            
            val savedAccumulation = pointAccumulationPersistencePort.save(accumulation)
            val accumulationId = savedAccumulation.id
                ?: throw IllegalStateException("적립 건 ID가 없습니다.")
            
            val result = service.use(memberId, orderNumber, amount)
            
            Then("1원 단위로 상세 내역이 생성되어야 한다") {
                val savedDetails = pointUsageDetailPersistencePort.findAll()
                savedDetails.size shouldBe 3 // 3원이므로 3개의 상세 내역
                savedDetails.forEach { detail ->
                    detail.amount.toLong() shouldBe 1L // 각 상세 내역은 1원
                    detail.pointAccumulationId shouldBe accumulationId
                    detail.pointUsageId shouldBe result.id
                }
            }
        }
    }
    
    Given("유효하지 않은 포인트 사용 요청이 있을 때") {
        When("0원 이하의 금액 사용 요청이 있을 때") {
            val memberId = 1L
            val orderNumber = "ORDER123"
            val amount = 0L
            
            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.use(memberId, orderNumber, amount)
                }
            }
        }
        
        When("음수 금액 사용 요청이 있을 때") {
            val memberId = 1L
            val orderNumber = "ORDER123"
            val amount = -1000L
            
            Then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    service.use(memberId, orderNumber, amount)
                }
            }
        }
    }
})
