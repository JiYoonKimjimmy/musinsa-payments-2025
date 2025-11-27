package com.musinsa.payments.point.application.event

import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakeMemberPointBalancePersistencePort
import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakePointAccumulationPersistencePort
import com.musinsa.payments.point.application.service.PointBalanceReconciliationService
import com.musinsa.payments.point.domain.entity.MemberPointBalance
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.event.BalanceReconciliationRequestEvent
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.test.TestDataGenerator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

/**
 * BalanceReconciliationEventHandler 단위 테스트
 */
class BalanceReconciliationEventHandlerTest : StringSpec({
    
    val memberPointBalancePersistencePort = FakeMemberPointBalancePersistencePort()
    val pointAccumulationPersistencePort = FakePointAccumulationPersistencePort()
    val reconciliationService = PointBalanceReconciliationService(
        memberPointBalancePersistencePort,
        pointAccumulationPersistencePort
    )
    val handler = BalanceReconciliationEventHandler(reconciliationService)

    "보정 요청 이벤트 수신 시 캐시와 실제 잔액이 불일치하면 보정되어야 한다" {
        // given
        val memberId = TestDataGenerator.randomMemberId()
        val pointKey1 = TestDataGenerator.randomPointKey()
        val pointKey2 = TestDataGenerator.randomPointKey()
        
        // 실제 적립 건 생성 (1500원)
        val accumulation1 = PointAccumulation(
            pointKey = pointKey1,
            memberId = memberId,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        val accumulation2 = PointAccumulation(
            pointKey = pointKey2,
            memberId = memberId,
            amount = Money.of(500L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        pointAccumulationPersistencePort.save(accumulation1)
        pointAccumulationPersistencePort.save(accumulation2)
        
        // 캐시된 잔액은 1000원 (불일치)
        val cachedBalance = MemberPointBalance(memberId)
        cachedBalance.addBalance(Money.of(1000L))
        memberPointBalancePersistencePort.save(cachedBalance)
        
        val event = BalanceReconciliationRequestEvent(
            memberId = memberId,
            reason = "캐시 업데이트 실패 (적립): DB 연결 실패",
            originalEventType = "적립"
        )
        
        // when
        handler.handleReconciliationRequest(event)
        
        // then
        val balance = memberPointBalancePersistencePort.findByMemberId(memberId)
        balance.isPresent shouldBe true
        balance.get().availableBalance shouldBe Money.of(1500L)
    }
    
    "보정 요청 이벤트 수신 시 캐시가 없으면 새로 생성되어야 한다" {
        // given
        val memberId = TestDataGenerator.randomMemberId()
        val pointKey = TestDataGenerator.randomPointKey()
        
        // 실제 적립 건 생성 (2000원)
        val accumulation = PointAccumulation(
            pointKey = pointKey,
            memberId = memberId,
            amount = Money.of(2000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        pointAccumulationPersistencePort.save(accumulation)
        
        // 캐시된 잔액 없음
        val event = BalanceReconciliationRequestEvent(
            memberId = memberId,
            reason = "캐시 업데이트 실패 (적립): DB 연결 실패",
            originalEventType = "적립"
        )
        
        // when
        handler.handleReconciliationRequest(event)
        
        // then
        val balance = memberPointBalancePersistencePort.findByMemberId(memberId)
        balance.isPresent shouldBe true
        balance.get().availableBalance shouldBe Money.of(2000L)
    }
    
    "보정 요청 이벤트 수신 시 캐시와 실제 잔액이 일치하면 변경되지 않아야 한다" {
        // given
        val memberId = TestDataGenerator.randomMemberId()
        val pointKey = TestDataGenerator.randomPointKey()
        
        // 실제 적립 건 생성 (1000원)
        val accumulation = PointAccumulation(
            pointKey = pointKey,
            memberId = memberId,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        pointAccumulationPersistencePort.save(accumulation)
        
        // 캐시된 잔액도 1000원 (일치)
        val cachedBalance = MemberPointBalance(memberId)
        cachedBalance.addBalance(Money.of(1000L))
        memberPointBalancePersistencePort.save(cachedBalance)
        
        val event = BalanceReconciliationRequestEvent(
            memberId = memberId,
            reason = "캐시 업데이트 실패 (사용): 락 타임아웃",
            originalEventType = "사용"
        )
        
        // when
        handler.handleReconciliationRequest(event)
        
        // then
        val balance = memberPointBalancePersistencePort.findByMemberId(memberId)
        balance.isPresent shouldBe true
        balance.get().availableBalance shouldBe Money.of(1000L)
    }
})

