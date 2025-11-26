package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakeMemberPointBalancePersistencePort
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointAccumulationPersistencePort
import com.musinsa.payments.point.domain.entity.MemberPointBalance
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.valueobject.Money
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

/**
 * PointBalanceReconciliationService 단위 테스트
 */
class PointBalanceReconciliationServiceTest : StringSpec({
    
    val memberPointBalancePersistencePort = FakeMemberPointBalancePersistencePort()
    val pointAccumulationPersistencePort = FakePointAccumulationPersistencePort()
    val service = PointBalanceReconciliationService(
        memberPointBalancePersistencePort,
        pointAccumulationPersistencePort
    )
    
    beforeEach {
        memberPointBalancePersistencePort.clear()
        pointAccumulationPersistencePort.clear()
    }
    
    "캐시된 잔액과 실제 잔액이 일치할 때 MATCHED 상태가 반환되어야 한다" {
        // given
        val memberId = 1L
        
        // 적립 건 생성
        val accumulation = PointAccumulation(
            pointKey = "TEST001",
            memberId = memberId,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        pointAccumulationPersistencePort.save(accumulation)
        
        // 캐시된 잔액 생성 (일치)
        val cachedBalance = MemberPointBalance(memberId)
        cachedBalance.addBalance(Money.of(1000L))
        memberPointBalancePersistencePort.save(cachedBalance)
        
        // when
        val result = service.reconcileMemberBalance(memberId)
        
        // then
        result.status shouldBe PointBalanceReconciliationService.ReconciliationStatus.MATCHED
        result.difference shouldBe Money.ZERO
    }
    
    "캐시된 잔액이 실제 잔액보다 적을 때 CORRECTED 상태가 반환되고 잔액이 보정되어야 한다" {
        // given
        val memberId = 1L
        
        // 적립 건 생성
        val accumulation1 = PointAccumulation(
            pointKey = "TEST001",
            memberId = memberId,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        val accumulation2 = PointAccumulation(
            pointKey = "TEST002",
            memberId = memberId,
            amount = Money.of(500L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        pointAccumulationPersistencePort.save(accumulation1)
        pointAccumulationPersistencePort.save(accumulation2)
        
        // 캐시된 잔액 생성 (불일치 - 1000원만 캐시됨)
        val cachedBalance = MemberPointBalance(memberId)
        cachedBalance.addBalance(Money.of(1000L))
        memberPointBalancePersistencePort.save(cachedBalance)
        
        // when
        val result = service.reconcileMemberBalance(memberId)
        
        // then
        result.status shouldBe PointBalanceReconciliationService.ReconciliationStatus.CORRECTED
        result.actualBalance shouldBe Money.of(1500L)
        result.cachedBalance shouldBe Money.of(1000L)
        result.difference shouldBe Money.of(500L)
        
        // 보정된 잔액 확인
        val correctedBalance = memberPointBalancePersistencePort.findByMemberId(memberId)
        correctedBalance.isPresent shouldBe true
        correctedBalance.get().availableBalance shouldBe Money.of(1500L)
    }
    
    "캐시된 잔액이 없고 실제 잔액이 있을 때 CREATED 상태가 반환되고 새 잔액이 생성되어야 한다" {
        // given
        val memberId = 1L
        
        // 적립 건 생성
        val accumulation = PointAccumulation(
            pointKey = "TEST001",
            memberId = memberId,
            amount = Money.of(2000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        pointAccumulationPersistencePort.save(accumulation)
        
        // when
        val result = service.reconcileMemberBalance(memberId)
        
        // then
        result.status shouldBe PointBalanceReconciliationService.ReconciliationStatus.CREATED
        result.actualBalance shouldBe Money.of(2000L)
        
        // 생성된 잔액 확인
        val newBalance = memberPointBalancePersistencePort.findByMemberId(memberId)
        newBalance.isPresent shouldBe true
        newBalance.get().availableBalance shouldBe Money.of(2000L)
    }
    
    "캐시된 잔액도 없고 실제 잔액도 없을 때 SKIPPED 상태가 반환되어야 한다" {
        // given
        val memberId = 999L
        
        // when
        val result = service.reconcileMemberBalance(memberId)
        
        // then
        result.status shouldBe PointBalanceReconciliationService.ReconciliationStatus.SKIPPED
        result.actualBalance shouldBe Money.ZERO
    }
})
