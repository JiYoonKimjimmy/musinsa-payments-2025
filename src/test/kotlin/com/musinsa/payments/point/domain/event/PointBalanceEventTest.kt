package com.musinsa.payments.point.domain.event

import com.musinsa.payments.point.domain.valueobject.Money
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDateTime

class PointBalanceEventTest : StringSpec({
    
    "Accumulated 이벤트를 생성할 수 있어야 한다" {
        // given
        val memberId = 1L
        val amount = Money.of(1000L)
        val pointKey = "TEST1234"
        
        // when
        val event = PointBalanceEvent.Accumulated(
            memberId = memberId,
            amount = amount,
            pointKey = pointKey
        )
        
        // then
        event.memberId shouldBe memberId
        event.amount shouldBe amount
        event.pointKey shouldBe pointKey
        event.shouldBeInstanceOf<PointBalanceEvent.Accumulated>()
    }
    
    "AccumulationCancelled 이벤트를 생성할 수 있어야 한다" {
        // given
        val memberId = 1L
        val amount = Money.of(1000L)
        val pointKey = "TEST1234"
        
        // when
        val event = PointBalanceEvent.AccumulationCancelled(
            memberId = memberId,
            amount = amount,
            pointKey = pointKey
        )
        
        // then
        event.memberId shouldBe memberId
        event.amount shouldBe amount
        event.pointKey shouldBe pointKey
        event.shouldBeInstanceOf<PointBalanceEvent.AccumulationCancelled>()
    }
    
    "Used 이벤트를 생성할 수 있어야 한다" {
        // given
        val memberId = 1L
        val amount = Money.of(500L)
        val pointKey = "USE12345"
        val orderNumber = "ORDER123"
        
        // when
        val event = PointBalanceEvent.Used(
            memberId = memberId,
            amount = amount,
            pointKey = pointKey,
            orderNumber = orderNumber
        )
        
        // then
        event.memberId shouldBe memberId
        event.amount shouldBe amount
        event.pointKey shouldBe pointKey
        event.orderNumber shouldBe orderNumber
        event.shouldBeInstanceOf<PointBalanceEvent.Used>()
    }
    
    "UsageCancelled 이벤트를 생성할 수 있어야 한다" {
        // given
        val memberId = 1L
        val amount = Money.of(500L)
        val pointKey = "USE12345"
        
        // when
        val event = PointBalanceEvent.UsageCancelled(
            memberId = memberId,
            amount = amount,
            pointKey = pointKey
        )
        
        // then
        event.memberId shouldBe memberId
        event.amount shouldBe amount
        event.pointKey shouldBe pointKey
        event.shouldBeInstanceOf<PointBalanceEvent.UsageCancelled>()
    }
    
    "Expired 이벤트를 생성할 수 있어야 한다" {
        // given
        val memberId = 1L
        val amount = Money.of(300L)
        val pointKey = "EXP12345"
        
        // when
        val event = PointBalanceEvent.Expired(
            memberId = memberId,
            amount = amount,
            pointKey = pointKey
        )
        
        // then
        event.memberId shouldBe memberId
        event.amount shouldBe amount
        event.pointKey shouldBe pointKey
        event.shouldBeInstanceOf<PointBalanceEvent.Expired>()
    }
    
    "이벤트 생성 시 occurredAt이 자동으로 설정되어야 한다" {
        // given
        val before = LocalDateTime.now()
        
        // when
        val event = PointBalanceEvent.Accumulated(
            memberId = 1L,
            amount = Money.of(1000L),
            pointKey = "TEST1234"
        )
        val after = LocalDateTime.now()
        
        // then
        event.occurredAt.isAfter(before.minusSeconds(1)) shouldBe true
        event.occurredAt.isBefore(after.plusSeconds(1)) shouldBe true
    }
    
    "모든 이벤트는 PointBalanceEvent의 하위 타입이어야 한다" {
        // given
        val events: List<PointBalanceEvent> = listOf(
            PointBalanceEvent.Accumulated(1L, Money.of(100L), "KEY1"),
            PointBalanceEvent.AccumulationCancelled(1L, Money.of(100L), "KEY2"),
            PointBalanceEvent.Used(1L, Money.of(100L), "KEY3", "ORDER1"),
            PointBalanceEvent.UsageCancelled(1L, Money.of(100L), "KEY4"),
            PointBalanceEvent.Expired(1L, Money.of(100L), "KEY5")
        )
        
        // when & then
        events.forEach { event ->
            event.shouldBeInstanceOf<PointBalanceEvent>()
        }
    }
})

