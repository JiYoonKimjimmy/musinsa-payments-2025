package com.musinsa.payments.point.application.integration

import com.musinsa.payments.point.application.port.input.PointAccumulationUseCase
import com.musinsa.payments.point.application.port.input.PointQueryUseCase
import com.musinsa.payments.point.application.port.input.PointUsageUseCase
import com.musinsa.payments.point.test.TestDataGenerator
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 적립 및 사용 통합 테스트
 * 
 * 복잡한 우선순위 시나리오 테스트:
 * - 시나리오 A: 수기 지급 + 일반 포인트 혼합 (5개 이상 적립 건)
 * - 시나리오 B: 일부 사용된 포인트 포함 복합 시나리오
 * - 시나리오 C: 대량 적립 건에서 선택적 사용
 */
@ActiveProfiles("test")
@Transactional
@SpringBootTest
class ScenarioAccumulationAndUsageTest @Autowired constructor(
    private val pointAccumulationUseCase: PointAccumulationUseCase,
    private val pointUsageUseCase: PointUsageUseCase,
    private val pointQueryUseCase: PointQueryUseCase
) : StringSpec({
    
    extensions(SpringExtension)
    
    "시나리오 A: 수기 지급 + 일반 포인트 혼합 (5개 이상 적립 건)에서 우선순위가 올바르게 적용되어야 한다" {
        val memberId = TestDataGenerator.randomMemberId()
        val orderNumber = TestDataGenerator.randomOrderNumber()
        
        // 수기 지급 포인트 2개 (만료일: 30일, 365일)
        val manualGrant30 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 5000L,
            expirationDays = 30,
            isManualGrant = true
        )
        
        val manualGrant365 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 3000L,
            expirationDays = 365,
            isManualGrant = true
        )
        
        // 일반 포인트 3개 (만료일: 10일, 100일, 200일)
        val normal10 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 2000L,
            expirationDays = 10,
            isManualGrant = false
        )
        
        val normal100 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 4000L,
            expirationDays = 100,
            isManualGrant = false
        )
        
        val normal200 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 3000L,
            expirationDays = 200,
            isManualGrant = false
        )
        
        // 총 적립: 5000 + 3000 + 2000 + 4000 + 3000 = 17000원
        val balanceBeforeUsage = pointQueryUseCase.getBalance(memberId)
        balanceBeforeUsage.totalBalance shouldBe 17000L
        balanceBeforeUsage.availableBalance shouldBe 17000L
        balanceBeforeUsage.accumulations shouldHaveSize 5
        
        // 12000원 사용 (수기 지급 30일 5000원 + 수기 지급 365일 3000원 + 일반 10일 2000원 + 일반 100일 2000원)
        val usage = pointUsageUseCase.use(
            memberId = memberId,
            orderNumber = orderNumber,
            amount = 12000L
        )
        
        usage.totalAmount.toLong() shouldBe 12000L
        
        // 잔액 확인
        val balanceAfterUsage = pointQueryUseCase.getBalance(memberId)
        balanceAfterUsage.totalBalance shouldBe 17000L
        balanceAfterUsage.availableBalance shouldBe 5000L  // 17000 - 12000 = 5000
        
        // 우선순위 검증: 수기 지급 포인트가 먼저 사용되고, 그 중 만료일 짧은 것부터 사용되어야 함
        // 1순위: 수기 지급 30일 (5000원 전액 사용)
        val manualGrant30After = balanceAfterUsage.accumulations.find { it.pointKey == manualGrant30.pointKey }
        manualGrant30After!!.availableAmount.toLong() shouldBe 0L
        
        // 2순위: 수기 지급 365일 (3000원 전액 사용)
        val manualGrant365After = balanceAfterUsage.accumulations.find { it.pointKey == manualGrant365.pointKey }
        manualGrant365After!!.availableAmount.toLong() shouldBe 0L
        
        // 3순위: 일반 포인트 10일 (2000원 전액 사용)
        val normal10After = balanceAfterUsage.accumulations.find { it.pointKey == normal10.pointKey }
        normal10After!!.availableAmount.toLong() shouldBe 0L
        
        // 4순위: 일반 포인트 100일 (2000원 사용, 잔액 2000원)
        val normal100After = balanceAfterUsage.accumulations.find { it.pointKey == normal100.pointKey }
        normal100After!!.availableAmount.toLong() shouldBe 2000L  // 4000 - 2000 = 2000
        
        // 사용되지 않음: 일반 포인트 200일 (3000원 그대로)
        val normal200After = balanceAfterUsage.accumulations.find { it.pointKey == normal200.pointKey }
        normal200After!!.availableAmount.toLong() shouldBe 3000L
    }
    
    "시나리오 B: 일부 사용된 포인트 포함 복합 시나리오에서 우선순위가 올바르게 적용되어야 한다" {
        val memberId = TestDataGenerator.randomMemberId()
        val orderNumber1 = TestDataGenerator.randomOrderNumber()
        val orderNumber2 = TestDataGenerator.randomOrderNumber()
        
        // 먼저 일부 포인트를 사용하여 일부 사용된 상태의 적립 건 생성
        // 수기 지급 포인트 5000원 적립 (만료일: 50일)
        val manualGrant = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 5000L,
            expirationDays = 50,
            isManualGrant = true
        )
        
        // 일반 포인트 3000원 적립 (만료일: 20일)
        val normal20 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 3000L,
            expirationDays = 20,
            isManualGrant = false
        )
        
        // 일반 포인트 4000원 적립 (만료일: 150일)
        val normal150 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 4000L,
            expirationDays = 150,
            isManualGrant = false
        )
        
        // 첫 번째 사용: 5000원 사용 (수기 지급 5000원 전액 사용)
        val firstUsage = pointUsageUseCase.use(
            memberId = memberId,
            orderNumber = orderNumber1,
            amount = 5000L
        )
        firstUsage.totalAmount.toLong() shouldBe 5000L
        
        // 일부 사용된 상태 확인
        val balanceAfterFirstUsage = pointQueryUseCase.getBalance(memberId)
        balanceAfterFirstUsage.totalBalance shouldBe 12000L
        balanceAfterFirstUsage.availableBalance shouldBe 7000L  // 12000 - 5000 = 7000
        
        val manualGrantAfterFirst = balanceAfterFirstUsage.accumulations.find { it.pointKey == manualGrant.pointKey }
        manualGrantAfterFirst!!.availableAmount.toLong() shouldBe 0L
        
        // 새로운 적립 건 추가
        // 수기 지급 포인트 2000원 적립 (만료일: 30일)
        val newManualGrant = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 2000L,
            expirationDays = 30,
            isManualGrant = true
        )
        
        // 일반 포인트 3000원 적립 (만료일: 10일)
        val newNormal = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 3000L,
            expirationDays = 10,
            isManualGrant = false
        )
        
        // 현재 상태: 총 적립 17000원, 사용 가능 12000원
        // (일부 사용된 수기 지급 0원, 일반 20일 3000원, 일반 150일 4000원, 새 수기 지급 2000원, 새 일반 3000원)
        val balanceBeforeSecondUsage = pointQueryUseCase.getBalance(memberId)
        balanceBeforeSecondUsage.totalBalance shouldBe 17000L
        balanceBeforeSecondUsage.availableBalance shouldBe 12000L
        
        // 두 번째 사용: 8000원 사용
        // 우선순위: 새 수기 지급 30일 2000원 + 새 일반 10일 3000원 + 일반 20일 3000원
        val secondUsage = pointUsageUseCase.use(
            memberId = memberId,
            orderNumber = orderNumber2,
            amount = 8000L
        )
        secondUsage.totalAmount.toLong() shouldBe 8000L
        
        // 최종 잔액 확인
        val balanceAfterSecondUsage = pointQueryUseCase.getBalance(memberId)
        balanceAfterSecondUsage.totalBalance shouldBe 17000L
        balanceAfterSecondUsage.availableBalance shouldBe 4000L  // 12000 - 8000 = 4000
        
        // 우선순위 검증
        // 일부 사용된 수기 지급 포인트는 이미 0원이므로 변경 없음
        val manualGrantAfterSecond = balanceAfterSecondUsage.accumulations.find { it.pointKey == manualGrant.pointKey }
        manualGrantAfterSecond!!.availableAmount.toLong() shouldBe 0L
        
        // 새 수기 지급 포인트는 전액 사용됨 (우선순위 1순위)
        val newManualGrantAfter = balanceAfterSecondUsage.accumulations.find { it.pointKey == newManualGrant.pointKey }
        newManualGrantAfter!!.availableAmount.toLong() shouldBe 0L
        
        // 새 일반 포인트 10일은 전액 사용됨 (일반 포인트 중 만료일이 가장 짧음)
        val newNormalAfter = balanceAfterSecondUsage.accumulations.find { it.pointKey == newNormal.pointKey }
        newNormalAfter!!.availableAmount.toLong() shouldBe 0L
        
        // 일반 포인트 20일은 전액 사용됨 (일반 포인트 중 두 번째로 짧음)
        val normal20After = balanceAfterSecondUsage.accumulations.find { it.pointKey == normal20.pointKey }
        normal20After!!.availableAmount.toLong() shouldBe 0L
        
        // 일반 포인트 150일은 사용되지 않음 (일반 포인트 중 만료일이 가장 김)
        val normal150After = balanceAfterSecondUsage.accumulations.find { it.pointKey == normal150.pointKey }
        normal150After!!.availableAmount.toLong() shouldBe 4000L
    }
    
    "시나리오 C: 대량 적립 건에서 선택적 사용 시 우선순위가 올바르게 적용되어야 한다" {
        val memberId = TestDataGenerator.randomMemberId()
        val orderNumber = TestDataGenerator.randomOrderNumber()
        
        // 수기 지급 포인트 3개 (다양한 만료일)
        val manualGrant10 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 2000L,
            expirationDays = 10,
            isManualGrant = true
        )
        
        val manualGrant50 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 3000L,
            expirationDays = 50,
            isManualGrant = true
        )
        
        val manualGrant200 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 4000L,
            expirationDays = 200,
            isManualGrant = true
        )
        
        // 일반 포인트 4개 (다양한 만료일)
        val normal5 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 1500L,
            expirationDays = 5,
            isManualGrant = false
        )
        
        val normal30 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 2500L,
            expirationDays = 30,
            isManualGrant = false
        )
        
        val normal100 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 3500L,
            expirationDays = 100,
            isManualGrant = false
        )
        
        val normal300 = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 4500L,
            expirationDays = 300,
            isManualGrant = false
        )
        
        // 총 적립: 2000 + 3000 + 4000 + 1500 + 2500 + 3500 + 4500 = 21000원
        val balanceBeforeUsage = pointQueryUseCase.getBalance(memberId)
        balanceBeforeUsage.totalBalance shouldBe 21000L
        balanceBeforeUsage.availableBalance shouldBe 21000L
        balanceBeforeUsage.accumulations shouldHaveSize 7
        
        // 중간 금액 사용: 10000원 사용
        // 예상 사용 순서:
        // 1. 수기 지급 10일 2000원 (전액)
        // 2. 수기 지급 50일 3000원 (전액)
        // 3. 수기 지급 200일 4000원 (전액)
        // 4. 일반 5일 1000원 (일부)
        val usage = pointUsageUseCase.use(
            memberId = memberId,
            orderNumber = orderNumber,
            amount = 10000L
        )
        
        usage.totalAmount.toLong() shouldBe 10000L
        
        // 잔액 확인
        val balanceAfterUsage = pointQueryUseCase.getBalance(memberId)
        balanceAfterUsage.totalBalance shouldBe 21000L
        balanceAfterUsage.availableBalance shouldBe 11000L  // 21000 - 10000 = 11000
        
        // 우선순위 검증: 수기 지급 포인트가 모두 사용되고, 일반 포인트 중 만료일 짧은 것이 일부 사용됨
        // 수기 지급 10일: 전액 사용
        val manualGrant10After = balanceAfterUsage.accumulations.find { it.pointKey == manualGrant10.pointKey }
        manualGrant10After!!.availableAmount.toLong() shouldBe 0L
        
        // 수기 지급 50일: 전액 사용
        val manualGrant50After = balanceAfterUsage.accumulations.find { it.pointKey == manualGrant50.pointKey }
        manualGrant50After!!.availableAmount.toLong() shouldBe 0L
        
        // 수기 지급 200일: 전액 사용
        val manualGrant200After = balanceAfterUsage.accumulations.find { it.pointKey == manualGrant200.pointKey }
        manualGrant200After!!.availableAmount.toLong() shouldBe 0L
        
        // 일반 5일: 일부 사용 (1000원 사용, 잔액 500원)
        val normal5After = balanceAfterUsage.accumulations.find { it.pointKey == normal5.pointKey }
        normal5After!!.availableAmount.toLong() shouldBe 500L  // 1500 - 1000 = 500
        
        // 일반 30일: 사용되지 않음
        val normal30After = balanceAfterUsage.accumulations.find { it.pointKey == normal30.pointKey }
        normal30After!!.availableAmount.toLong() shouldBe 2500L
        
        // 일반 100일: 사용되지 않음
        val normal100After = balanceAfterUsage.accumulations.find { it.pointKey == normal100.pointKey }
        normal100After!!.availableAmount.toLong() shouldBe 3500L
        
        // 일반 300일: 사용되지 않음
        val normal300After = balanceAfterUsage.accumulations.find { it.pointKey == normal300.pointKey }
        normal300After!!.availableAmount.toLong() shouldBe 4500L
    }
})

