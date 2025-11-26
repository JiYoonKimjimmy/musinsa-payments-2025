package com.musinsa.payments.point.domain.entity

import com.musinsa.payments.point.domain.valueobject.Money
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MemberPointBalanceTest : StringSpec({
    
    "MemberPointBalance를 생성할 수 있어야 한다" {
        // given
        val memberId = 1L
        
        // when
        val balance = MemberPointBalance(memberId)
        
        // then
        balance.memberId shouldBe memberId
        balance.availableBalance shouldBe Money.ZERO
        balance.totalAccumulated shouldBe Money.ZERO
        balance.totalUsed shouldBe Money.ZERO
        balance.totalExpired shouldBe Money.ZERO
    }
    
    "포인트 적립 시 잔액과 총 적립액이 증가해야 한다" {
        // given
        val balance = MemberPointBalance(1L)
        val amount = Money.of(1000L)
        
        // when
        balance.addBalance(amount)
        
        // then
        balance.availableBalance shouldBe Money.of(1000L)
        balance.totalAccumulated shouldBe Money.of(1000L)
    }
    
    "포인트 사용 시 잔액이 감소하고 총 사용액이 증가해야 한다" {
        // given
        val balance = MemberPointBalance(1L)
        balance.addBalance(Money.of(1000L))
        val usageAmount = Money.of(300L)
        
        // when
        balance.subtractBalance(usageAmount)
        
        // then
        balance.availableBalance shouldBe Money.of(700L)
        balance.totalUsed shouldBe Money.of(300L)
    }
    
    "잔액 부족 시 사용하면 예외가 발생해야 한다" {
        // given
        val balance = MemberPointBalance(1L)
        balance.addBalance(Money.of(500L))
        
        // when & then
        shouldThrow<IllegalArgumentException> {
            balance.subtractBalance(Money.of(600L))
        }
    }
    
    "포인트 복원 시 잔액이 증가해야 한다" {
        // given
        val balance = MemberPointBalance(1L)
        balance.addBalance(Money.of(1000L))
        balance.subtractBalance(Money.of(500L))
        val restoreAmount = Money.of(300L)
        
        // when
        balance.restoreBalance(restoreAmount)
        
        // then
        balance.availableBalance shouldBe Money.of(800L)
        // totalUsed는 이력 보존을 위해 감소하지 않음
        balance.totalUsed shouldBe Money.of(500L)
    }
    
    "적립 취소 시 잔액이 감소해야 한다" {
        // given
        val balance = MemberPointBalance(1L)
        balance.addBalance(Money.of(1000L))
        val cancelAmount = Money.of(400L)
        
        // when
        balance.cancelAccumulation(cancelAmount)
        
        // then
        balance.availableBalance shouldBe Money.of(600L)
        // totalAccumulated는 이력 보존을 위해 감소하지 않음
        balance.totalAccumulated shouldBe Money.of(1000L)
    }
    
    "포인트 만료 시 잔액이 감소하고 총 만료액이 증가해야 한다" {
        // given
        val balance = MemberPointBalance(1L)
        balance.addBalance(Money.of(1000L))
        val expireAmount = Money.of(200L)
        
        // when
        balance.expireBalance(expireAmount)
        
        // then
        balance.availableBalance shouldBe Money.of(800L)
        balance.totalExpired shouldBe Money.of(200L)
    }
    
    "잔액 보정이 정상적으로 동작해야 한다" {
        // given
        val balance = MemberPointBalance(1L)
        balance.addBalance(Money.of(1000L))
        val actualBalance = Money.of(800L)
        
        // when
        balance.reconcile(actualBalance)
        
        // then
        balance.availableBalance shouldBe Money.of(800L)
    }
    
    "0원 이하 금액으로 적립하면 예외가 발생해야 한다" {
        // given
        val balance = MemberPointBalance(1L)
        
        // when & then
        shouldThrow<IllegalArgumentException> {
            balance.addBalance(Money.ZERO)
        }
    }
    
    "0원 이하 금액으로 사용하면 예외가 발생해야 한다" {
        // given
        val balance = MemberPointBalance(1L)
        balance.addBalance(Money.of(1000L))
        
        // when & then
        shouldThrow<IllegalArgumentException> {
            balance.subtractBalance(Money.ZERO)
        }
    }
    
    "restore 메서드로 기존 데이터를 복원할 수 있어야 한다" {
        // given
        val memberId = 1L
        val availableBalance = Money.of(5000L)
        val totalAccumulated = Money.of(10000L)
        val totalUsed = Money.of(5000L)
        val totalExpired = Money.of(0L)
        val version = 5L
        
        // when
        val balance = MemberPointBalance.restore(
            memberId = memberId,
            availableBalance = availableBalance,
            totalAccumulated = totalAccumulated,
            totalUsed = totalUsed,
            totalExpired = totalExpired,
            version = version,
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now()
        )
        
        // then
        balance.memberId shouldBe memberId
        balance.availableBalance shouldBe availableBalance
        balance.totalAccumulated shouldBe totalAccumulated
        balance.totalUsed shouldBe totalUsed
        balance.totalExpired shouldBe totalExpired
        balance.version shouldBe version
    }
})

