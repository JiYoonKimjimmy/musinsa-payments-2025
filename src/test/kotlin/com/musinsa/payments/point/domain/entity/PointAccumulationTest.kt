package com.musinsa.payments.point.domain.entity

import com.musinsa.payments.point.domain.exception.CannotCancelAccumulationException
import com.musinsa.payments.point.domain.exception.InsufficientPointException
import com.musinsa.payments.point.domain.exception.InvalidAmountException
import com.musinsa.payments.point.domain.valueobject.Money
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

class PointAccumulationTest : StringSpec({
    
    "PointAccumulation을 생성할 수 있어야 한다" {
        // given
        val pointKey = "TEST1234"
        val memberId = 1L
        val amount = Money.of(1000L)
        val expirationDate = LocalDate.now().plusDays(365)
        
        // when
        val accumulation = PointAccumulation(
            pointKey = pointKey,
            memberId = memberId,
            amount = amount,
            expirationDate = expirationDate
        )
        
        // then
        accumulation.pointKey shouldBe pointKey
        accumulation.memberId shouldBe memberId
        accumulation.amount shouldBe amount
        accumulation.availableAmount shouldBe amount
        accumulation.expirationDate shouldBe expirationDate
        accumulation.status shouldBe PointAccumulationStatus.ACCUMULATED
        accumulation.isManualGrant shouldBe false
    }
    
    "생성 시 사용 가능 잔액은 적립 금액과 동일해야 한다" {
        // given
        val amount = Money.of(1000L)
        
        // when
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = amount,
            expirationDate = LocalDate.now().plusDays(365)
        )
        
        // then
        accumulation.availableAmount shouldBe amount
    }
    
    "사용되지 않은 적립은 취소 가능해야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        
        // when
        val result = accumulation.canBeCancelled()
        
        // then
        result shouldBe true
    }
    
    "일부 사용된 적립은 취소 불가해야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.use(Money.of(500L))
        
        // when
        val result = accumulation.canBeCancelled()
        
        // then
        result shouldBe false
    }
    
    "이미 취소된 적립은 취소 불가해야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.cancel()
        
        // when
        val result = accumulation.canBeCancelled()
        
        // then
        result shouldBe false
    }
    
    "포인트 사용 시 사용 가능 잔액이 차감되어야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        val usageAmount = Money.of(300L)
        
        // when
        accumulation.use(usageAmount)
        
        // then
        accumulation.availableAmount shouldBe Money.of(700L)
    }
    
    "사용 가능 잔액이 부족하면 예외가 발생해야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.use(Money.of(500L))
        val usageAmount = Money.of(600L)
        
        // when & then
        val exception = shouldThrow<InsufficientPointException> {
            accumulation.use(usageAmount)
        }
        exception.shouldBeInstanceOf<InsufficientPointException>()
    }
    
    "만료일이 지난 경우 만료 여부가 true여야 한다" {
        // given - 오늘 날짜로 생성 (이미 만료된 것으로 간주)
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now()
        )
        
        // when - 다음 날 기준으로 만료 여부 확인
        val result = accumulation.isExpiredAt(LocalDate.now().plusDays(1))
        
        // then
        result shouldBe true
    }
    
    "만료일이 아직 지나지 않은 경우 만료 여부가 false여야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(1)
        )
        
        // when
        val result = accumulation.isExpired()
        
        // then
        result shouldBe false
    }
    
    "특정 날짜 기준으로 만료 여부를 확인할 수 있어야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(10)
        )
        val checkDate = LocalDate.now().plusDays(11)
        
        // when
        val result = accumulation.isExpiredAt(checkDate)
        
        // then
        result shouldBe true
    }
    
    "적립 취소가 정상적으로 동작해야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        
        // when
        accumulation.cancel()
        
        // then
        accumulation.status shouldBe PointAccumulationStatus.CANCELLED
    }
    
    "사용된 적립을 취소하려고 하면 예외가 발생해야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.use(Money.of(500L))
        
        // when & then
        shouldThrow<CannotCancelAccumulationException> {
            accumulation.cancel()
        }
    }
    
    "만료 처리 시 상태가 EXPIRED로 변경되어야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        
        // when
        accumulation.markAsExpired()
        
        // then
        accumulation.status shouldBe PointAccumulationStatus.EXPIRED
    }
    
    "사용 가능 잔액이 있으면 hasAvailableAmount가 true여야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        
        // when
        val result = accumulation.hasAvailableAmount()
        
        // then
        result shouldBe true
    }
    
    "사용 가능 잔액이 0이면 hasAvailableAmount가 false여야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.use(Money.of(1000L))
        
        // when
        val result = accumulation.hasAvailableAmount()
        
        // then
        result shouldBe false
    }
    
    "포인트 복원이 정상적으로 동작해야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.use(Money.of(500L))
        val restoreAmount = Money.of(300L)
        
        // when
        accumulation.restore(restoreAmount)
        
        // then
        accumulation.availableAmount shouldBe Money.of(800L)
    }
    
    "0원 이하 복원 시 예외가 발생해야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        
        // when & then - 0원 복원 시 예외
        shouldThrow<InvalidAmountException> {
            accumulation.restore(Money.ZERO)
        }
        
        // 음수는 Money 생성 시점에 예외가 발생하므로 별도로 테스트할 수 없음
        // (Money.of(-100L)은 생성 시 IllegalArgumentException 발생)
    }
    
    "복원 후 사용 가능 잔액이 적립 금액을 초과하면 예외가 발생해야 한다" {
        // given
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365)
        )
        accumulation.use(Money.of(200L))
        val restoreAmount = Money.of(900L)  // 800 + 900 = 1700 > 1000
        
        // when & then
        shouldThrow<InvalidAmountException> {
            accumulation.restore(restoreAmount)
        }
    }
    
    "수기 지급 포인트를 생성할 수 있어야 한다" {
        // given & when
        val accumulation = PointAccumulation(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = LocalDate.now().plusDays(365),
            isManualGrant = true
        )
        
        // then
        accumulation.isManualGrant shouldBe true
    }
    
    "포인트 키가 비어있으면 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointAccumulation(
                pointKey = "",
                memberId = 1L,
                amount = Money.of(1000L),
                expirationDate = LocalDate.now().plusDays(365)
            )
        }
    }
    
    "회원 ID가 0 이하면 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointAccumulation(
                pointKey = "TEST1234",
                memberId = 0L,
                amount = Money.of(1000L),
                expirationDate = LocalDate.now().plusDays(365)
            )
        }
    }
    
    "적립 금액이 0원 이하면 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointAccumulation(
                pointKey = "TEST1234",
                memberId = 1L,
                amount = Money.ZERO,
                expirationDate = LocalDate.now().plusDays(365)
            )
        }
    }
    
    "만료일이 오늘 이전이면 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointAccumulation(
                pointKey = "TEST1234",
                memberId = 1L,
                amount = Money.of(1000L),
                expirationDate = LocalDate.now().minusDays(1)
            )
        }
    }
})

