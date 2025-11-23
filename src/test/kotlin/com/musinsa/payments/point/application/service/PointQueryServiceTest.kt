package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDate

/**
 * PointQueryService 단위 테스트
 */
class PointQueryServiceTest : BehaviorSpec({
    
    val pointAccumulationPersistencePort = mockk<PointAccumulationPersistencePort>()
    val pointUsagePersistencePort = mockk<PointUsagePersistencePort>()
    
    val service = PointQueryService(
        pointAccumulationPersistencePort,
        pointUsagePersistencePort
    )
    
    Given("사용 가능한 포인트 적립 내역이 있을 때") {
        val memberId = 1L
        
        val accumulation1 = PointAccumulation(
            pointKey = "ACCUM01",
            memberId = memberId,
            amount = Money.of(10000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation1.availableAmount = Money.of(8000L) // 일부 사용됨
        
        val accumulation2 = PointAccumulation(
            pointKey = "ACCUM02",
            memberId = memberId,
            amount = Money.of(5000L),
            expirationDate = LocalDate.now().plusDays(200)
        )
        accumulation2.availableAmount = Money.of(5000L)
        
        every { pointAccumulationPersistencePort.findByMemberIdAndStatus(memberId, PointAccumulationStatus.ACCUMULATED) } returns listOf(accumulation1, accumulation2)
        
        When("포인트 잔액을 조회하면") {
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
        
        val validAccumulation = PointAccumulation(
            pointKey = "ACCUM01",
            memberId = memberId,
            amount = Money.of(10000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        validAccumulation.availableAmount = Money.of(10000L)
        
        // 만료된 적립 건은 생성 후 만료일을 변경해야 함 (생성자는 만료일 검증을 하므로)
        val expiredAccumulation = PointAccumulation(
            pointKey = "ACCUM02",
            memberId = memberId,
            amount = Money.of(5000L),
            expirationDate = LocalDate.now().plusDays(1) // 일단 유효한 날짜로 생성
        )
        expiredAccumulation.availableAmount = Money.of(3000L)
        expiredAccumulation.expirationDate = LocalDate.now().minusDays(1)
        
        every { pointAccumulationPersistencePort.findByMemberIdAndStatus(memberId, PointAccumulationStatus.ACCUMULATED) } returns listOf(validAccumulation, expiredAccumulation)
        
        When("포인트 잔액을 조회하면") {
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
        
        every { pointAccumulationPersistencePort.findByMemberIdAndStatus(memberId, PointAccumulationStatus.ACCUMULATED) } returns emptyList()
        
        When("포인트 잔액을 조회하면") {
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
        
        val usage1 = PointUsage(
            pointKey = "USAGE01",
            memberId = memberId,
            orderNumber = OrderNumber.of("ORDER123"),
            totalAmount = Money.of(5000L)
        )
        
        val usage2 = PointUsage(
            pointKey = "USAGE02",
            memberId = memberId,
            orderNumber = OrderNumber.of("ORDER456"),
            totalAmount = Money.of(3000L)
        )
        
        val page = PageImpl(listOf(usage1, usage2), pageable, 2)
        
        every { pointUsagePersistencePort.findUsageHistoryByMemberId(memberId, null, pageable) } returns page
        
        When("사용 내역을 조회하면") {
            val result = service.getUsageHistory(memberId, null, pageable)
            
            Then("사용 내역이 정확히 조회되어야 한다") {
                result.content.size shouldBe 2
                result.totalElements shouldBe 2
                result.content[0].pointKey shouldBe "USAGE01"
                result.content[1].pointKey shouldBe "USAGE02"
            }
        }
    }
    
    Given("특정 주문번호의 사용 내역이 있을 때") {
        val memberId = 1L
        val orderNumber = "ORDER123"
        val pageable = PageRequest.of(0, 10)
        
        val usage = PointUsage(
            pointKey = "USAGE01",
            memberId = memberId,
            orderNumber = OrderNumber.of(orderNumber),
            totalAmount = Money.of(5000L)
        )
        
        val page = PageImpl(listOf(usage), pageable, 1)
        
        every { pointUsagePersistencePort.findUsageHistoryByMemberId(memberId, orderNumber, pageable) } returns page
        
        When("주문번호로 필터링하여 조회하면") {
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
        
        val usages = (1..5).map { i ->
            PointUsage(
                pointKey = "USAGE0$i",
                memberId = memberId,
                orderNumber = OrderNumber.of("ORDER$i"),
                totalAmount = Money.of(1000L * i)
            )
        }
        
        val page = PageImpl(usages, pageable, 15) // 총 15개, 두 번째 페이지
        
        every { pointUsagePersistencePort.findUsageHistoryByMemberId(memberId, null, pageable) } returns page
        
        When("페이징된 사용 내역을 조회하면") {
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
