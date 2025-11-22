package com.musinsa.payments.point.domain.valueobject

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.math.BigDecimal

class MoneyTest : StringSpec({
    
    "Long 값으로 Money를 생성할 수 있어야 한다" {
        // given & when
        val money = Money.of(1000L)
        
        // then
        money.toLong() shouldBe 1000L
    }
    
    "BigDecimal 값으로 Money를 생성할 수 있어야 한다" {
        // given & when
        val money = Money.of(BigDecimal("1000.50"))
        
        // then
        money.toBigDecimal() shouldBe BigDecimal("1000")
    }
    
    "ZERO 상수를 사용할 수 있어야 한다" {
        // given & when
        val zero = Money.ZERO
        
        // then
        zero.toLong() shouldBe 0L
    }
    
    "금액 덧셈이 정상적으로 동작해야 한다" {
        // given
        val money1 = Money.of(1000L)
        val money2 = Money.of(500L)
        
        // when
        val result = money1.add(money2)
        
        // then
        result.toLong() shouldBe 1500L
    }
    
    "금액 뺄셈이 정상적으로 동작해야 한다" {
        // given
        val money1 = Money.of(1000L)
        val money2 = Money.of(300L)
        
        // when
        val result = money1.subtract(money2)
        
        // then
        result.toLong() shouldBe 700L
    }
    
    "음수 금액 생성 시 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            Money.of(-100L)
        }
    }
    
    "음수 BigDecimal로 Money 생성 시 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            Money.of(BigDecimal("-100"))
        }
    }
    
    "0원은 생성할 수 있어야 한다" {
        // given & when
        val zero = Money.of(0L)
        
        // then
        zero shouldBe Money.ZERO
    }
    
    "금액이 더 큰지 확인할 수 있어야 한다" {
        // given
        val money1 = Money.of(1000L)
        val money2 = Money.of(500L)
        
        // when & then
        money1.isGreaterThan(money2) shouldBe true
        money2.isGreaterThan(money1) shouldBe false
    }
    
    "금액이 더 크거나 같은지 확인할 수 있어야 한다" {
        // given
        val money1 = Money.of(1000L)
        val money2 = Money.of(1000L)
        val money3 = Money.of(500L)
        
        // when & then
        money1.isGreaterThanOrEqual(money2) shouldBe true
        money1.isGreaterThanOrEqual(money3) shouldBe true
        money3.isGreaterThanOrEqual(money1) shouldBe false
    }
    
    "금액이 더 작은지 확인할 수 있어야 한다" {
        // given
        val money1 = Money.of(500L)
        val money2 = Money.of(1000L)
        
        // when & then
        money1.isLessThan(money2) shouldBe true
        money2.isLessThan(money1) shouldBe false
    }
    
    "금액이 더 작거나 같은지 확인할 수 있어야 한다" {
        // given
        val money1 = Money.of(1000L)
        val money2 = Money.of(1000L)
        val money3 = Money.of(500L)
        
        // when & then
        money3.isLessThanOrEqual(money1) shouldBe true
        money1.isLessThanOrEqual(money2) shouldBe true
        money1.isLessThanOrEqual(money3) shouldBe false
    }
    
    "같은 금액의 Money는 equals로 비교할 수 있어야 한다" {
        // given
        val money1 = Money.of(1000L)
        val money2 = Money.of(1000L)
        
        // when & then
        money1 shouldBe money2
    }
    
    "다른 금액의 Money는 equals로 비교하면 false여야 한다" {
        // given
        val money1 = Money.of(1000L)
        val money2 = Money.of(500L)
        
        // when & then
        money1 shouldNotBe money2
    }
    
    "BigDecimal 소수점은 내림 처리되어야 한다" {
        // given
        val money = Money.of(BigDecimal("1000.99"))
        
        // when & then
        money.toLong() shouldBe 1000L
    }
    
    "Money는 불변 객체여야 한다" {
        // given
        val money1 = Money.of(1000L)
        val money2 = Money.of(500L)
        
        // when
        val result = money1.add(money2)
        
        // then
        money1.toLong() shouldBe 1000L  // 원본이 변경되지 않아야 함
        result.toLong() shouldBe 1500L
    }
})

