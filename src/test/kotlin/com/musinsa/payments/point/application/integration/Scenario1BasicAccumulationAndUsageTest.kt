package com.musinsa.payments.point.application.integration

import com.musinsa.payments.point.application.port.input.PointAccumulationUseCase
import com.musinsa.payments.point.application.port.input.PointQueryUseCase
import com.musinsa.payments.point.application.port.input.PointUsageUseCase
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * 시나리오 1: 기본 적립 및 사용 테스트
 * 
 * 테스트 시나리오:
 * 1. 1000원 적립 (pointKey: A)
 * 2. 500원 적립 (pointKey: B)
 * 3. 주문번호 A1234에서 1200원 사용 (pointKey: C)
 * 4. 검증: 총 잔액 300원, A에서 1000원 사용, B에서 200원 사용
 */
@Transactional
@ActiveProfiles("test")
@SpringBootTest
class Scenario1BasicAccumulationAndUsageTest @Autowired constructor(
    private val pointAccumulationUseCase: PointAccumulationUseCase,
    private val pointUsageUseCase: PointUsageUseCase,
    private val pointQueryUseCase: PointQueryUseCase
) : StringSpec({
    
    extensions(SpringExtension)
    
    "시나리오 1: 기본 적립 및 사용이 정상적으로 동작해야 한다" {
        val memberId = 1L
        
        // 1. 1000원 적립 (pointKey: A)
        val accumulationA = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 1000L,
            expirationDays = 365,
            isManualGrant = false
        )
        
        accumulationA.pointKey shouldNotBe null
        accumulationA.memberId shouldBe memberId
        accumulationA.amount.toLong() shouldBe 1000L
        accumulationA.availableAmount.toLong() shouldBe 1000L
        accumulationA.status shouldBe PointAccumulationStatus.ACCUMULATED
        
        val pointKeyA = accumulationA.pointKey
        
        // 잔액 조회 - 총 잔액 1000원
        val balanceAfterA = pointQueryUseCase.getBalance(memberId)
        balanceAfterA.totalBalance shouldBe 1000L
        balanceAfterA.availableBalance shouldBe 1000L
        
        // 2. 500원 적립 (pointKey: B)
        val accumulationB = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 500L,
            expirationDays = 365,
            isManualGrant = false
        )
        
        accumulationB.pointKey shouldNotBe null
        accumulationB.pointKey shouldNotBe pointKeyA  // 다른 키여야 함
        accumulationB.memberId shouldBe memberId
        accumulationB.amount.toLong() shouldBe 500L
        accumulationB.availableAmount.toLong() shouldBe 500L
        
        val pointKeyB = accumulationB.pointKey
        
        // 잔액 조회 - 총 잔액 1500원
        val balanceAfterB = pointQueryUseCase.getBalance(memberId)
        balanceAfterB.totalBalance shouldBe 1500L
        balanceAfterB.availableBalance shouldBe 1500L
        balanceAfterB.accumulations shouldHaveSize 2
        
        // 3. 주문번호 A1234에서 1200원 사용 (pointKey: C)
        val usage = pointUsageUseCase.use(
            memberId = memberId,
            orderNumber = "A1234",
            amount = 1200L
        )
        
        usage.pointKey shouldNotBe null
        usage.memberId shouldBe memberId
        usage.orderNumber.value shouldBe "A1234"
        usage.totalAmount.toLong() shouldBe 1200L
        
        // pointKeyC는 검증에 사용되지 않으므로 주석 처리
        
        // 잔액 조회 - 총 잔액 300원 (1500 - 1200)
        val balanceAfterUsage = pointQueryUseCase.getBalance(memberId)
        balanceAfterUsage.totalBalance shouldBe 1500L  // 총 적립 금액은 변하지 않음
        balanceAfterUsage.availableBalance shouldBe 300L  // 사용 가능 잔액만 감소
        
        // 적립 내역 확인
        val accumulationsAfterUsage = balanceAfterUsage.accumulations
        accumulationsAfterUsage shouldHaveSize 2
        
        // A 적립 건 확인: 1000원에서 1000원 사용 (사용 가능 잔액 0원)
        val accumulationAAfterUsage = accumulationsAfterUsage.find { it.pointKey == pointKeyA }
        accumulationAAfterUsage shouldNotBe null
        accumulationAAfterUsage!!.amount.toLong() shouldBe 1000L
        accumulationAAfterUsage.availableAmount.toLong() shouldBe 0L  // 모두 사용됨
        
        // B 적립 건 확인: 500원에서 200원 사용 (사용 가능 잔액 300원)
        val accumulationBAfterUsage = accumulationsAfterUsage.find { it.pointKey == pointKeyB }
        accumulationBAfterUsage shouldNotBe null
        accumulationBAfterUsage!!.amount.toLong() shouldBe 500L
        accumulationBAfterUsage.availableAmount.toLong() shouldBe 300L  // 500 - 200 = 300
    }
    
    "시나리오 1 확장: 여러 적립 건에서 순차적으로 사용할 때 우선순위가 올바르게 적용되어야 한다" {
        val memberId = 2L
        
        // 수기 지급 포인트 1000원 (우선 사용)
        val manualGrant = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 1000L,
            expirationDays = 365,
            isManualGrant = true
        )
        
        // 일반 포인트 500원 (만료일이 더 짧음)
        val normalShortExpiry = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 500L,
            expirationDays = 100,
            isManualGrant = false
        )
        
        // 일반 포인트 500원 (만료일이 더 김)
        val normalLongExpiry = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 500L,
            expirationDays = 365,
            isManualGrant = false
        )
        
        // 1200원 사용 (수기 지급 1000원 + 만료일 짧은 것 200원)
        val usage = pointUsageUseCase.use(
            memberId = memberId,
            orderNumber = "ORDER001",
            amount = 1200L
        )
        
        usage.totalAmount.toLong() shouldBe 1200L
        
        // 잔액 확인
        val balance = pointQueryUseCase.getBalance(memberId)
        balance.totalBalance shouldBe 2000L
        balance.availableBalance shouldBe 800L  // 2000 - 1200 = 800
        
        // 수기 지급 포인트는 모두 사용되어야 함
        val manualGrantAfter = balance.accumulations.find { it.pointKey == manualGrant.pointKey }
        manualGrantAfter!!.availableAmount.toLong() shouldBe 0L
        
        // 만료일 짧은 포인트는 일부 사용되어야 함
        val normalShortAfter = balance.accumulations.find { it.pointKey == normalShortExpiry.pointKey }
        normalShortAfter!!.availableAmount.toLong() shouldBe 300L  // 500 - 200 = 300
        
        // 만료일 긴 포인트는 사용되지 않아야 함
        val normalLongAfter = balance.accumulations.find { it.pointKey == normalLongExpiry.pointKey }
        normalLongAfter!!.availableAmount.toLong() shouldBe 500L
    }
})

