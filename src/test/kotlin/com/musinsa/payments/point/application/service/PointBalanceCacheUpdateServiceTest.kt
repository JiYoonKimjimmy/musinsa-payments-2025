package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakeMemberPointBalancePersistencePort
import com.musinsa.payments.point.domain.entity.MemberPointBalance
import com.musinsa.payments.point.domain.event.BalanceReconciliationRequestEvent
import com.musinsa.payments.point.domain.event.PointBalanceEvent
import com.musinsa.payments.point.domain.valueobject.Money
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.context.ApplicationEventPublisher

/**
 * PointBalanceCacheUpdateService 단위 테스트
 * 
 * Note: 재시도 로직(@Retryable)은 Spring 프록시를 통해 동작하므로,
 * 통합 테스트에서 검증합니다. 이 테스트는 기본 동작을 검증합니다.
 */
class PointBalanceCacheUpdateServiceTest : StringSpec({
    
    val memberPointBalancePersistencePort = FakeMemberPointBalancePersistencePort()
    val eventCaptor = EventCapturingPublisher()
    val service = PointBalanceCacheUpdateService(memberPointBalancePersistencePort, eventCaptor)
    
    beforeEach {
        memberPointBalancePersistencePort.clear()
        eventCaptor.clear()
    }
    
    "잔액이 없는 회원에 대해 적립 처리 시 새로운 잔액이 생성되어야 한다" {
        // given
        val memberId = 1L
        val event = PointBalanceEvent.Accumulated(
            memberId = memberId,
            amount = Money.of(1000L),
            pointKey = "TEST001"
        )
        
        // when
        service.updateBalanceWithRetry(event) { it.addBalance(event.amount) }
        
        // then
        val balance = memberPointBalancePersistencePort.findByMemberId(memberId)
        balance.isPresent shouldBe true
        balance.get().availableBalance shouldBe Money.of(1000L)
    }
    
    "기존 잔액이 있는 회원에 대해 적립 처리 시 잔액이 증가해야 한다" {
        // given
        val memberId = 1L
        val initialBalance = MemberPointBalance(memberId)
        initialBalance.addBalance(Money.of(5000L))
        memberPointBalancePersistencePort.save(initialBalance)
        
        val event = PointBalanceEvent.Accumulated(
            memberId = memberId,
            amount = Money.of(1000L),
            pointKey = "TEST002"
        )
        
        // when
        service.updateBalanceWithRetry(event) { it.addBalance(event.amount) }
        
        // then
        val balance = memberPointBalancePersistencePort.findByMemberId(memberId)
        balance.get().availableBalance shouldBe Money.of(6000L)
    }
    
    "사용 처리 시 잔액이 감소해야 한다" {
        // given
        val memberId = 1L
        val initialBalance = MemberPointBalance(memberId)
        initialBalance.addBalance(Money.of(5000L))
        memberPointBalancePersistencePort.save(initialBalance)
        
        val event = PointBalanceEvent.Used(
            memberId = memberId,
            amount = Money.of(2000L),
            pointKey = "USE001",
            orderNumber = "ORDER001"
        )
        
        // when
        service.updateBalanceWithRetry(event) { it.subtractBalance(event.amount) }
        
        // then
        val balance = memberPointBalancePersistencePort.findByMemberId(memberId)
        balance.get().availableBalance shouldBe Money.of(3000L)
        balance.get().totalUsed shouldBe Money.of(2000L)
    }
    
    "복구 메서드 호출 시 보정 요청 이벤트가 발행되어야 한다" {
        // given
        val memberId = 1L
        val event = PointBalanceEvent.Accumulated(
            memberId = memberId,
            amount = Money.of(1000L),
            pointKey = "TEST003"
        )
        val exception = RuntimeException("DB 연결 실패")
        
        // when
        service.recoverFromFailure(exception, event) { it.addBalance(event.amount) }
        
        // then
        eventCaptor.capturedEvents.size shouldBe 1
        val capturedEvent = eventCaptor.capturedEvents.first()
        capturedEvent shouldNotBe null
        
        val reconciliationEvent = capturedEvent as BalanceReconciliationRequestEvent
        reconciliationEvent.memberId shouldBe memberId
        reconciliationEvent.originalEventType shouldBe "적립"
        reconciliationEvent.reason shouldBe "캐시 업데이트 실패 (적립): DB 연결 실패"
    }
})

/**
 * 테스트용 이벤트 캡처 발행자
 */
class EventCapturingPublisher : ApplicationEventPublisher {
    val capturedEvents = mutableListOf<Any>()
    
    override fun publishEvent(event: Any) {
        capturedEvents.add(event)
    }
    
    fun clear() {
        capturedEvents.clear()
    }
}

