package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.config.fixtures.FakePointConfigPort
import com.musinsa.payments.point.application.port.output.fixtures.FakePointKeyGenerator
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointUsageDetailPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointUsageStatus
import com.musinsa.payments.point.domain.entity.fixtures.PointAccumulationFixture
import com.musinsa.payments.point.domain.entity.fixtures.PointUsageDetailFixture
import com.musinsa.payments.point.domain.entity.fixtures.PointUsageFixture
import com.musinsa.payments.point.domain.exception.CannotCancelUsageException
import com.musinsa.payments.point.domain.valueobject.Money
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * PointCancellationService 단위 테스트
 */
class PointCancellationServiceTest : BehaviorSpec({

    val pointUsagePersistencePort = FakePointUsagePersistencePort()
    val pointUsageDetailPersistencePort = FakePointUsageDetailPersistencePort(pointUsagePersistencePort)
    val pointAccumulationPersistencePort = FakePointAccumulationPersistencePort()
    val pointKeyGenerator = FakePointKeyGenerator()
    val pointConfigPort = FakePointConfigPort().apply { setupDefaultConfigs() }

    val service = PointCancellationService(
        pointUsagePersistencePort,
        pointUsageDetailPersistencePort,
        pointAccumulationPersistencePort,
        pointKeyGenerator,
        pointConfigPort
    )

    beforeContainer {
        pointUsagePersistencePort.clear()
        pointUsageDetailPersistencePort.clear()
        pointAccumulationPersistencePort.clear()
        pointKeyGenerator.resetCounter()
        pointConfigPort.resetToDefaults()
    }
    
    Given("취소 가능한 포인트 사용 건이 있을 때") {
        val pointKey = "USAGE01"
        val memberId = 1L
        val orderNumber = "ORDER123"
        val totalAmount = Money.of(5000L)

        When("전체 취소하면") {
            // 데이터 준비 - 적립 건 생성
            val accumulation = PointAccumulationFixture.create(
                pointKey = "ACCUM01",
                memberId = memberId,
                amount = 10000L
            )
            val savedAccumulation = pointAccumulationPersistencePort.save(accumulation)
            val accumulationId = savedAccumulation.id
                ?: throw IllegalStateException("적립 건 ID가 없습니다.")

            // 적립 건 사용 처리
            savedAccumulation.use(totalAmount)
            pointAccumulationPersistencePort.save(savedAccumulation)

            // 사용 건 생성
            val usage = PointUsageFixture.createWithMoney(
                pointKey = pointKey,
                memberId = memberId,
                orderNumber = orderNumber,
                totalAmount = totalAmount
            )
            val savedUsage = pointUsagePersistencePort.save(usage)
            val usageId = savedUsage.id
                ?: throw IllegalStateException("사용 건 ID가 없습니다.")

            // 사용 상세 내역 생성 (1원 단위로 totalAmount 만큼 생성)
            val usageDetails = PointUsageDetailFixture.createMultipleOneWon(
                pointUsageId = usageId,
                pointAccumulationId = accumulationId,
                count = totalAmount.toLong()
            )
            pointUsageDetailPersistencePort.saveAll(usageDetails)

            // 취소 전 적립 건 상태 확인
            val beforeCancel = pointAccumulationPersistencePort.findById(accumulationId).orElseThrow()
            beforeCancel.availableAmount shouldBe Money.of(5000L) // 5000원 사용 후

            val result = service.cancelUsage(pointKey)

            Then("사용 건이 FULLY_CANCELLED 상태로 변경되어야 한다") {
                result.status shouldBe PointUsageStatus.FULLY_CANCELLED
                result.cancelledAmount shouldBe totalAmount

                // 적립 건이 복원되었는지 확인
                val restoredAccumulation = pointAccumulationPersistencePort.findById(accumulationId).orElseThrow()
                restoredAccumulation.availableAmount shouldBe Money.of(10000L) // 원래 금액으로 복원
            }
        }
    }
    
    Given("부분 취소 가능한 포인트 사용 건이 있을 때") {
        val pointKey = "USAGE02"
        val memberId = 1L
        val orderNumber = "ORDER123"
        val totalAmount = Money.of(10000L)
        val cancelAmount = 3000L

        When("부분 취소하면") {
            // 데이터 준비 - 적립 건 생성
            val accumulation = PointAccumulationFixture.create(
                pointKey = "ACCUM01",
                memberId = memberId,
                amount = 20000L
            )
            val savedAccumulation = pointAccumulationPersistencePort.save(accumulation)
            val accumulationId = savedAccumulation.id
                ?: throw IllegalStateException("적립 건 ID가 없습니다.")

            // 적립 건 사용 처리
            savedAccumulation.use(totalAmount)
            pointAccumulationPersistencePort.save(savedAccumulation)

            // 사용 건 생성
            val usage = PointUsageFixture.createWithMoney(
                pointKey = pointKey,
                memberId = memberId,
                orderNumber = orderNumber,
                totalAmount = totalAmount
            )
            val savedUsage = pointUsagePersistencePort.save(usage)
            val usageId = savedUsage.id
                ?: throw IllegalStateException("사용 건 ID가 없습니다.")

            // 사용 상세 내역 생성 (1원 단위로 totalAmount 만큼 생성)
            val usageDetails = PointUsageDetailFixture.createMultipleOneWon(
                pointUsageId = usageId,
                pointAccumulationId = accumulationId,
                count = totalAmount.toLong()
            )
            pointUsageDetailPersistencePort.saveAll(usageDetails)

            val result = service.cancelUsage(pointKey, cancelAmount)

            Then("사용 건이 PARTIALLY_CANCELLED 상태로 변경되어야 한다") {
                result.status shouldBe PointUsageStatus.PARTIALLY_CANCELLED
                result.cancelledAmount.toLong() shouldBe cancelAmount

                // 적립 건이 부분 복원되었는지 확인
                val restoredAccumulation = pointAccumulationPersistencePort.findById(accumulationId).orElseThrow()
                restoredAccumulation.availableAmount shouldBe Money.of(10000L + cancelAmount) // 20000 - 10000 + 3000
            }
        }
    }
    
    Given("존재하지 않는 사용 건 키가 있을 때") {
        val pointKey = "NOTFOUND"

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

        When("취소하면") {
            // 데이터 준비 - 사용 건만 생성 (적립 건은 필요 없음)
            val usage = PointUsageFixture.createWithMoney(
                pointKey = pointKey,
                memberId = memberId,
                orderNumber = orderNumber,
                totalAmount = totalAmount
            )
            pointUsagePersistencePort.save(usage)

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

        When("취소하면") {
            // 데이터 준비 - 만료된 적립 건 생성
            val expiredAccumulation = PointAccumulationFixture.createExpired(
                pointKey = "EXPIRED01",
                memberId = memberId,
                amount = 10000L,
                availableAmount = 10000L - totalAmount.toLong(),
                daysAgo = 1L
            )
            val savedExpiredAccumulation = pointAccumulationPersistencePort.save(expiredAccumulation)
            val expiredAccumulationId = savedExpiredAccumulation.id
                ?: throw IllegalStateException("적립 건 ID가 없습니다.")

            // 사용 처리
            savedExpiredAccumulation.use(totalAmount)
            pointAccumulationPersistencePort.save(savedExpiredAccumulation)

            // 사용 건 생성
            val usage = PointUsageFixture.createWithMoney(
                pointKey = pointKey,
                memberId = memberId,
                orderNumber = orderNumber,
                totalAmount = totalAmount
            )
            val savedUsage = pointUsagePersistencePort.save(usage)
            val usageId = savedUsage.id
                ?: throw IllegalStateException("사용 건 ID가 없습니다.")

            // 사용 상세 내역 생성 (1원 단위로 totalAmount 만큼 생성)
            val usageDetails = PointUsageDetailFixture.createMultipleOneWon(
                pointUsageId = usageId,
                pointAccumulationId = expiredAccumulationId,
                count = totalAmount.toLong()
            )
            pointUsageDetailPersistencePort.saveAll(usageDetails)

            val result = service.cancelUsage(pointKey)

            Then("만료된 포인트는 신규 적립으로 처리되어야 한다") {
                result.status shouldBe PointUsageStatus.FULLY_CANCELLED

                // 신규 적립 건이 생성되었는지 확인
                val allAccumulations = pointAccumulationPersistencePort.findAll()
                val newAccumulations = allAccumulations.filter {
                    it.id != expiredAccumulationId && it.memberId == memberId
                }
                newAccumulations.size shouldBe 1
                newAccumulations.first().amount shouldBe totalAmount
            }
        }
    }
    
    Given("만료되지 않은 포인트가 포함된 사용 건이 있을 때") {
        val pointKey = "USAGE05"
        val memberId = 1L
        val orderNumber = "ORDER123"
        val totalAmount = Money.of(5000L)

        When("취소하면") {
            // 데이터 준비 - 적립 건 생성
            val accumulation = PointAccumulationFixture.create(
                pointKey = "ACCUM01",
                memberId = memberId,
                amount = 10000L
            )
            val savedAccumulation = pointAccumulationPersistencePort.save(accumulation)
            val accumulationId = savedAccumulation.id
                ?: throw IllegalStateException("적립 건 ID가 없습니다.")

            // 적립 건 사용 처리
            savedAccumulation.use(totalAmount)
            pointAccumulationPersistencePort.save(savedAccumulation)

            // 사용 건 생성
            val usage = PointUsageFixture.createWithMoney(
                pointKey = pointKey,
                memberId = memberId,
                orderNumber = orderNumber,
                totalAmount = totalAmount
            )
            val savedUsage = pointUsagePersistencePort.save(usage)
            val usageId = savedUsage.id
                ?: throw IllegalStateException("사용 건 ID가 없습니다.")

            // 사용 상세 내역 생성 (1원 단위로 totalAmount 만큼 생성)
            val usageDetails = PointUsageDetailFixture.createMultipleOneWon(
                pointUsageId = usageId,
                pointAccumulationId = accumulationId,
                count = totalAmount.toLong()
            )
            pointUsageDetailPersistencePort.saveAll(usageDetails)

            service.cancelUsage(pointKey)

            Then("기존 적립 건이 복원되어야 한다") {
                // 적립 건이 원래 금액으로 복원되었는지 확인
                val restoredAccumulation = pointAccumulationPersistencePort.findById(accumulationId).orElseThrow()
                restoredAccumulation.availableAmount shouldBe Money.of(10000L) // 원래 금액으로 복원
            }
        }
    }
})
