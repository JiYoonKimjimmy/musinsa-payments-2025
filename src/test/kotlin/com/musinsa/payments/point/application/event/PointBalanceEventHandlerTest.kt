package com.musinsa.payments.point.application.event

import com.musinsa.payments.point.application.port.output.persistence.fixtures.FakeMemberPointBalancePersistencePort
import com.musinsa.payments.point.application.service.PointBalanceCacheUpdateService
import com.musinsa.payments.point.domain.entity.MemberPointBalance
import com.musinsa.payments.point.domain.event.PointBalanceEvent
import com.musinsa.payments.point.domain.valueobject.Money
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.context.ApplicationEventPublisher

/**
 * PointBalanceEventHandler 단위 테스트
 */
class PointBalanceEventHandlerTest : StringSpec({
    
    val memberPointBalancePersistencePort = FakeMemberPointBalancePersistencePort()
    val eventPublisher = NoOpEventPublisher()
    val cacheUpdateService = PointBalanceCacheUpdateService(memberPointBalancePersistencePort, eventPublisher)
    val handler = PointBalanceEventHandler(cacheUpdateService)
    
    beforeEach {
        memberPointBalancePersistencePort.clear()
    }
    
    "회원의 잔액이 없을 때 포인트 적립 이벤트가 발생하면 새로운 잔액이 생성되어야 한다" {
        // given
        val memberId = 1L
        val event = PointBalanceEvent.Accumulated(
            memberId = memberId,
            amount = Money.of(1000L),
            pointKey = "TEST001"
        )
        
        // when
        handler.handleAccumulated(event)
        
        // then
        val balance = memberPointBalancePersistencePort.findByMemberId(memberId)
        balance.isPresent shouldBe true
        balance.get().availableBalance shouldBe Money.of(1000L)
        balance.get().totalAccumulated shouldBe Money.of(1000L)
    }
    
    "회원의 잔액이 있을 때 포인트 적립 이벤트가 발생하면 기존 잔액에 추가되어야 한다" {
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
        handler.handleAccumulated(event)
        
        // then
        val balance = memberPointBalancePersistencePort.findByMemberId(memberId)
        balance.isPresent shouldBe true
        balance.get().availableBalance shouldBe Money.of(6000L)
    }
    
    "회원에게 적립된 포인트가 있을 때 포인트 사용 이벤트가 발생하면 잔액이 차감되어야 한다" {
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
        handler.handleUsed(event)
        
        // then
        val balance = memberPointBalancePersistencePort.findByMemberId(memberId)
        balance.isPresent shouldBe true
        balance.get().availableBalance shouldBe Money.of(3000L)
        balance.get().totalUsed shouldBe Money.of(2000L)
    }
    
    "포인트를 사용한 회원이 있을 때 사용 취소 이벤트가 발생하면 잔액이 복원되어야 한다" {
        // given
        val memberId = 1L
        val initialBalance = MemberPointBalance(memberId)
        initialBalance.addBalance(Money.of(5000L))
        initialBalance.subtractBalance(Money.of(2000L))
        memberPointBalancePersistencePort.save(initialBalance)
        
        val event = PointBalanceEvent.UsageCancelled(
            memberId = memberId,
            amount = Money.of(1000L),
            pointKey = "CANCEL001"
        )
        
        // when
        handler.handleUsageCancelled(event)
        
        // then
        val balance = memberPointBalancePersistencePort.findByMemberId(memberId)
        balance.isPresent shouldBe true
        balance.get().availableBalance shouldBe Money.of(4000L)
    }
    
    "적립된 포인트가 있을 때 적립 취소 이벤트가 발생하면 잔액이 차감되어야 한다" {
        // given
        val memberId = 1L
        val initialBalance = MemberPointBalance(memberId)
        initialBalance.addBalance(Money.of(5000L))
        memberPointBalancePersistencePort.save(initialBalance)
        
        val event = PointBalanceEvent.AccumulationCancelled(
            memberId = memberId,
            amount = Money.of(2000L),
            pointKey = "ACC_CANCEL001"
        )
        
        // when
        handler.handleAccumulationCancelled(event)
        
        // then
        val balance = memberPointBalancePersistencePort.findByMemberId(memberId)
        balance.isPresent shouldBe true
        balance.get().availableBalance shouldBe Money.of(3000L)
    }
    
    "만료 예정 포인트가 있을 때 만료 이벤트가 발생하면 잔액이 차감되고 만료액이 증가해야 한다" {
        // given
        val memberId = 1L
        val initialBalance = MemberPointBalance(memberId)
        initialBalance.addBalance(Money.of(5000L))
        memberPointBalancePersistencePort.save(initialBalance)
        
        val event = PointBalanceEvent.Expired(
            memberId = memberId,
            amount = Money.of(1000L),
            pointKey = "EXP001"
        )
        
        // when
        handler.handleExpired(event)
        
        // then
        val balance = memberPointBalancePersistencePort.findByMemberId(memberId)
        balance.isPresent shouldBe true
        balance.get().availableBalance shouldBe Money.of(4000L)
        balance.get().totalExpired shouldBe Money.of(1000L)
    }
})

/**
 * 테스트용 No-Op 이벤트 발행자
 */
private class NoOpEventPublisher : ApplicationEventPublisher {
    override fun publishEvent(event: Any) {
        // 테스트에서는 이벤트를 무시
    }
}
