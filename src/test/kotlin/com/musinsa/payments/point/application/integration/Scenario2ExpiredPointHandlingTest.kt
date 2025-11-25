package com.musinsa.payments.point.application.integration

import com.musinsa.payments.point.application.port.input.PointAccumulationUseCase
import com.musinsa.payments.point.application.port.input.PointCancellationUseCase
import com.musinsa.payments.point.application.port.input.PointQueryUseCase
import com.musinsa.payments.point.application.port.input.PointUsageUseCase
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointAccumulationJpaRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 시나리오 2: 만료 포인트 처리 테스트
 * 
 * 테스트 시나리오:
 * 1. 포인트 적립 (A: 1000원, B: 500원)
 * 2. 포인트 사용 (1200원: A에서 1000원, B에서 200원 사용)
 * 3. A 적립 만료 처리 (만료일을 과거로 변경)
 * 4. 사용 취소 (1100원)
 * 5. 검증: A는 만료되었으므로 신규 적립 처리, B는 복원
 */
@Transactional
@Import(PointEntityMapper::class)
@ActiveProfiles("test")
@SpringBootTest
class Scenario2ExpiredPointHandlingTest @Autowired constructor(
    private val pointAccumulationUseCase: PointAccumulationUseCase,
    private val pointUsageUseCase: PointUsageUseCase,
    private val pointCancellationUseCase: PointCancellationUseCase,
    private val pointQueryUseCase: PointQueryUseCase,
    private val pointAccumulationJpaRepository: PointAccumulationJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : StringSpec({
    
    extensions(SpringExtension)
    
    "시나리오 2: 만료 포인트 사용 취소가 정상적으로 동작해야 한다" {
        val memberId = 1L
        
        // 1. 포인트 적립 (A: 1000원, B: 500원)
        val accumulationA = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 1000L,
            expirationDays = 365,
            isManualGrant = false
        )
        val pointKeyA = accumulationA.pointKey
        
        val accumulationB = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 500L,
            expirationDays = 365,
            isManualGrant = false
        )
        val pointKeyB = accumulationB.pointKey
        
        // 초기 잔액 확인
        val initialBalance = pointQueryUseCase.getBalance(memberId)
        initialBalance.totalBalance shouldBe 1500L
        initialBalance.availableBalance shouldBe 1500L
        
        // 2. 포인트 사용 (1200원: A에서 1000원, B에서 200원 사용)
        val usage = pointUsageUseCase.use(
            memberId = memberId,
            orderNumber = "ORDER001",
            amount = 1200L
        )
        val pointKeyC = usage.pointKey
        
        usage.totalAmount.toLong() shouldBe 1200L
        
        // 사용 후 잔액 확인
        val balanceAfterUsage = pointQueryUseCase.getBalance(memberId)
        balanceAfterUsage.totalBalance shouldBe 1500L
        balanceAfterUsage.availableBalance shouldBe 300L  // 1500 - 1200 = 300
        
        // A는 모두 사용됨 (0원 남음)
        val accumulationAAfterUsage = balanceAfterUsage.accumulations.find { it.pointKey == pointKeyA }
        accumulationAAfterUsage!!.availableAmount.toLong() shouldBe 0L
        
        // B는 일부 사용됨 (300원 남음)
        val accumulationBAfterUsage = balanceAfterUsage.accumulations.find { it.pointKey == pointKeyB }
        accumulationBAfterUsage!!.availableAmount.toLong() shouldBe 300L
        
        // 3. A 적립 만료 처리 (만료일을 과거로 변경)
        val accumulationAEntity = pointAccumulationJpaRepository.findByPointKey(pointKeyA).orElseThrow()
        accumulationAEntity.expirationDate = LocalDate.now().minusDays(1)  // 만료일을 어제로 설정
        pointAccumulationJpaRepository.save(accumulationAEntity)
        
        // 만료 확인
        val accumulationAAfterExpiry = pointEntityMapper.toDomain(
            pointAccumulationJpaRepository.findByPointKey(pointKeyA).orElseThrow()
        )
        accumulationAAfterExpiry.isExpired() shouldBe true
        
        // 4. 사용 취소 (1100원: A에서 사용한 1000원 + B에서 사용한 100원)
        val cancelledUsage = pointCancellationUseCase.cancelUsage(
            pointKey = pointKeyC,
            amount = 1100L,
            reason = "주문 취소"
        )
        
        cancelledUsage.cancelledAmount.toLong() shouldBe 1100L
        cancelledUsage.getRemainingAmount().toLong() shouldBe 100L  // 1200 - 1100 = 100
        
        // 5. 검증: A는 만료되었으므로 신규 적립 처리, B는 복원
        val balanceAfterCancel = pointQueryUseCase.getBalance(memberId)
        
        // 총 잔액은 1500원 + 신규 적립 1000원 = 2500원
        // (A는 만료되어 신규 적립으로 처리되었고, B는 복원됨)
        balanceAfterCancel.totalBalance shouldBe 2500L
        
        // 사용 가능 잔액: A 신규 적립 1000원 + B 복원 후 400원 = 1400원
        balanceAfterCancel.availableBalance shouldBe 1400L
        
        // 적립 건 개수 확인: 원래 A, B + 신규 적립 A' = 3개
        val allAccumulations = balanceAfterCancel.accumulations
        allAccumulations shouldHaveSize 3
        
        // 원래 A는 만료되어 사용 가능 잔액 0원
        val originalA = allAccumulations.find { it.pointKey == pointKeyA && it.availableAmount.toLong() == 0L }
        originalA shouldNotBe null
        
        // 신규 적립 A' (만료 포인트로부터 생성) 확인
        val newAccumulationFromA = allAccumulations.find { 
            it.pointKey != pointKeyA && 
            it.pointKey != pointKeyB && 
            it.amount.toLong() == 1000L &&
            it.availableAmount.toLong() == 1000L
        }
        newAccumulationFromA shouldNotBe null
        newAccumulationFromA!!.status shouldBe PointAccumulationStatus.ACCUMULATED
        
        // B는 복원되어 400원 (300원 남은 것 + 100원 복원)
        val accumulationBAfterCancel = allAccumulations.find { it.pointKey == pointKeyB }
        accumulationBAfterCancel!!.availableAmount.toLong() shouldBe 400L  // 300 + 100 = 400
    }
    
    "시나리오 2 확장: 만료되지 않은 포인트는 복원되어야 한다" {
        val memberId = 2L
        
        // 포인트 적립
        val accumulation = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 1000L,
            expirationDays = 365,
            isManualGrant = false
        )
        val pointKey = accumulation.pointKey
        
        // 포인트 사용
        val usage = pointUsageUseCase.use(
            memberId = memberId,
            orderNumber = "ORDER002",
            amount = 500L
        )
        
        // 사용 후 잔액 확인
        val balanceAfterUsage = pointQueryUseCase.getBalance(memberId)
        balanceAfterUsage.availableBalance shouldBe 500L
        
        // 사용 취소 (만료되지 않은 포인트)
        val cancelledUsage = pointCancellationUseCase.cancelUsage(
            pointKey = usage.pointKey,
            amount = 500L,
            reason = "주문 취소"
        )
        
        cancelledUsage.cancelledAmount.toLong() shouldBe 500L
        
        // 검증: 만료되지 않은 포인트는 복원되어야 함 (신규 적립 생성되지 않음)
        val balanceAfterCancel = pointQueryUseCase.getBalance(memberId)
        balanceAfterCancel.totalBalance shouldBe 1000L  // 신규 적립 없음
        balanceAfterCancel.availableBalance shouldBe 1000L  // 모두 복원됨
        
        // 적립 건 개수는 여전히 1개
        balanceAfterCancel.accumulations shouldHaveSize 1
        
        // 원래 적립 건이 복원됨
        val restoredAccumulation = balanceAfterCancel.accumulations[0]
        restoredAccumulation.pointKey shouldBe pointKey
        restoredAccumulation.availableAmount.toLong() shouldBe 1000L
    }
    
    "시나리오 2 확장: 부분 취소 시 만료 포인트는 신규 적립으로 처리되어야 한다" {
        val memberId = 3L
        
        // 포인트 적립
        val accumulation = pointAccumulationUseCase.accumulate(
            memberId = memberId,
            amount = 1000L,
            expirationDays = 365,
            isManualGrant = false
        )
        val pointKey = accumulation.pointKey
        
        // 포인트 사용
        val usage = pointUsageUseCase.use(
            memberId = memberId,
            orderNumber = "ORDER003",
            amount = 1000L
        )
        
        // 만료 처리
        val accumulationEntity = pointAccumulationJpaRepository.findByPointKey(pointKey).orElseThrow()
        accumulationEntity.expirationDate = LocalDate.now().minusDays(1)
        pointAccumulationJpaRepository.save(accumulationEntity)
        
        // 부분 취소 (500원)
        val cancelledUsage = pointCancellationUseCase.cancelUsage(
            pointKey = usage.pointKey,
            amount = 500L,
            reason = "부분 취소"
        )
        
        cancelledUsage.cancelledAmount.toLong() shouldBe 500L
        
        // 검증: 만료 포인트 500원은 신규 적립으로 처리됨
        val balanceAfterCancel = pointQueryUseCase.getBalance(memberId)
        balanceAfterCancel.totalBalance shouldBe 1500L  // 원래 1000원 + 신규 적립 500원
        balanceAfterCancel.availableBalance shouldBe 500L  // 신규 적립 500원만 사용 가능
        
        // 적립 건 개수: 원래 1개 + 신규 적립 1개 = 2개
        balanceAfterCancel.accumulations shouldHaveSize 2
        
        // 신규 적립 확인
        val newAccumulation = balanceAfterCancel.accumulations.find { 
            it.pointKey != pointKey && 
            it.amount.toLong() == 500L
        }
        newAccumulation shouldNotBe null
        newAccumulation!!.availableAmount.toLong() shouldBe 500L
    }
})

