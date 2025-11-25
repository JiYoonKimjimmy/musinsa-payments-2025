package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.entity.fixtures.PointAccumulationFixture
import com.musinsa.payments.point.domain.entity.fixtures.PointUsageFixture
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * PointQueryService 단위 테스트
 */
class PointQueryServiceTest : BehaviorSpec({

    val pointAccumulationPersistencePort = FakePointAccumulationPersistencePort()
    val pointUsagePersistencePort = FakePointUsagePersistencePort()

    val service = PointQueryService(
        pointAccumulationPersistencePort,
        pointUsagePersistencePort
    )

    beforeContainer {
        pointAccumulationPersistencePort.clear()
        pointUsagePersistencePort.clear()
    }
    
    Given("사용 가능한 포인트 적립 내역이 있을 때") {
        val memberId = 1L

        When("포인트 잔액을 조회하면") {
            // 데이터 준비: 일부 사용된 적립 건
            val accumulation1 = PointAccumulationFixture.createPartiallyUsed(
                pointKey = "ACCUM01",
                memberId = memberId,
                amount = 10000L,
                availableAmount = 8000L
            )
            pointAccumulationPersistencePort.save(accumulation1)

            // 데이터 준비: 전액 사용 가능한 적립 건
            val accumulation2 = PointAccumulationFixture.createExpiringSoon(
                pointKey = "ACCUM02",
                memberId = memberId,
                amount = 5000L,
                daysUntilExpiration = 200
            )
            pointAccumulationPersistencePort.save(accumulation2)

            val result = service.getBalance(memberId)

            Then("잔액 정보가 정확히 계산되어야 한다") {
                result.memberId shouldBe memberId
                result.totalBalance shouldBe 15000L // 10000 + 5000
                result.availableBalance shouldBe 13000L // 8000 + 5000
                result.expiredBalance shouldBe 0L
                result.accumulations.size shouldBe 2
            }
        }
    }
    
    Given("만료된 포인트가 포함된 적립 내역이 있을 때") {
        val memberId = 1L

        When("포인트 잔액을 조회하면") {
            // 데이터 준비: 유효한 적립 건
            val validAccumulation = PointAccumulationFixture.create(
                pointKey = "ACCUM01",
                memberId = memberId,
                amount = 10000L
            )
            pointAccumulationPersistencePort.save(validAccumulation)

            // 데이터 준비: 만료된 적립 건
            val expiredAccumulation = PointAccumulationFixture.createExpired(
                pointKey = "ACCUM02",
                memberId = memberId,
                amount = 5000L,
                availableAmount = 3000L,
                daysAgo = 1L
            )
            pointAccumulationPersistencePort.save(expiredAccumulation)

            val result = service.getBalance(memberId)

            Then("만료된 포인트와 사용 가능한 포인트가 구분되어야 한다") {
                result.totalBalance shouldBe 15000L // 10000 + 5000
                result.availableBalance shouldBe 10000L // 만료되지 않은 것만
                result.expiredBalance shouldBe 3000L // 만료된 것만
            }
        }
    }
    
    Given("적립 내역이 없을 때") {
        val memberId = 1L

        When("포인트 잔액을 조회하면") {
            // 데이터 준비: 없음 (beforeContainer에서 clear됨)
            val result = service.getBalance(memberId)

            Then("모든 잔액이 0이어야 한다") {
                result.memberId shouldBe memberId
                result.totalBalance shouldBe 0L
                result.availableBalance shouldBe 0L
                result.expiredBalance shouldBe 0L
                result.accumulations.isEmpty() shouldBe true
            }
        }
    }
    
    Given("포인트 사용 내역이 있을 때") {
        val memberId = 1L
        val pageable = PageRequest.of(0, 10)

        When("사용 내역을 조회하면") {
            // 데이터 준비: 사용 내역 2건 (명시적으로 다른 createdAt 설정)
            val baseTime = LocalDateTime.now()
            val usage1 = PointUsage(
                pointKey = "USAGE01",
                memberId = memberId,
                orderNumber = OrderNumber.of("ORDER123"),
                totalAmount = Money.of(5000L),
                createdAt = baseTime.minusSeconds(1)
            )
            pointUsagePersistencePort.save(usage1)

            val usage2 = PointUsage(
                pointKey = "USAGE02",
                memberId = memberId,
                orderNumber = OrderNumber.of("ORDER456"),
                totalAmount = Money.of(3000L),
                createdAt = baseTime
            )
            pointUsagePersistencePort.save(usage2)

            val result = service.getUsageHistory(memberId, null, pageable)

            Then("사용 내역이 정확히 조회되어야 한다") {
                result.content.size shouldBe 2
                result.totalElements shouldBe 2
                // createdAt 내림차순이므로 나중에 저장된 것이 먼저 조회됨
                result.content[0].pointKey shouldBe "USAGE02"
                result.content[1].pointKey shouldBe "USAGE01"
            }
        }
    }
    
    Given("특정 주문번호의 사용 내역이 있을 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val pageable = PageRequest.of(0, 10)

        When("주문번호로 필터링하여 조회하면") {
            // 데이터 준비: 특정 주문번호의 사용 내역
            val usage = PointUsageFixture.create(
                pointKey = "USAGE01",
                memberId = memberId,
                orderNumber = orderNumber,
                totalAmount = 5000L
            )
            pointUsagePersistencePort.save(usage)

            val result = service.getUsageHistory(memberId, orderNumber, pageable)

            Then("해당 주문번호의 사용 내역만 조회되어야 한다") {
                result.content.size shouldBe 1
                result.content[0].orderNumber.value shouldBe orderNumber
            }
        }
    }
    
    Given("페이징이 필요한 사용 내역이 있을 때") {
        val memberId = 1L
        val pageable = PageRequest.of(1, 5) // 두 번째 페이지, 페이지당 5개

        When("페이징된 사용 내역을 조회하면") {
            // 데이터 준비: 총 15개의 사용 내역
            (1..15).forEach { i ->
                val usage = PointUsageFixture.create(
                    pointKey = "USAGE${String.format("%02d", i)}",
                    memberId = memberId,
                    orderNumber = "ORDER$i",
                    totalAmount = 1000L * i
                )
                pointUsagePersistencePort.save(usage)
            }

            val result = service.getUsageHistory(memberId, null, pageable)

            Then("페이징 정보가 정확해야 한다") {
                result.content.size shouldBe 5
                result.totalElements shouldBe 15
                result.number shouldBe 1
                result.size shouldBe 5
            }
        }
    }
})
