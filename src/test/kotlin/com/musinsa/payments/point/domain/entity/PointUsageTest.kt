package com.musinsa.payments.point.domain.entity

import com.musinsa.payments.point.domain.exception.CannotCancelUsageException
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PointUsageTest : StringSpec({
    
    "PointUsage를 생성할 수 있어야 한다" {
        // given
        val pointKey = "USAGE1234"
        val memberId = 1L
        val orderNumber = OrderNumber.of("ORDER1234")
        val totalAmount = Money.of(1000L)
        
        // when
        val usage = PointUsage(
            pointKey = pointKey,
            memberId = memberId,
            orderNumber = orderNumber,
            totalAmount = totalAmount
        )
        
        // then
        usage.pointKey shouldBe pointKey
        usage.memberId shouldBe memberId
        usage.orderNumber shouldBe orderNumber
        usage.totalAmount shouldBe totalAmount
        usage.cancelledAmount shouldBe Money.ZERO
        usage.status shouldBe PointUsageStatus.USED
    }
    
    "남은 사용 금액을 계산할 수 있어야 한다" {
        // given
        val usage = PointUsage(
            pointKey = "USAGE1234",
            memberId = 1L,
            orderNumber = OrderNumber.of("ORDER1234"),
            totalAmount = Money.of(1000L)
        )
        usage.cancel(Money.of(300L))
        
        // when
        val remaining = usage.getRemainingAmount()
        
        // then
        remaining shouldBe Money.of(700L)
    }
    
    "취소 가능 여부를 확인할 수 있어야 한다" {
        // given
        val usage = PointUsage(
            pointKey = "USAGE1234",
            memberId = 1L,
            orderNumber = OrderNumber.of("ORDER1234"),
            totalAmount = Money.of(1000L)
        )
        
        // when & then
        usage.canCancel(Money.of(500L)) shouldBe true
        usage.canCancel(Money.of(1000L)) shouldBe true
        usage.canCancel(Money.of(1500L)) shouldBe false
    }
    
    "사용 취소가 정상적으로 동작해야 한다" {
        // given
        val usage = PointUsage(
            pointKey = "USAGE1234",
            memberId = 1L,
            orderNumber = OrderNumber.of("ORDER1234"),
            totalAmount = Money.of(1000L)
        )
        
        // when
        usage.cancel(Money.of(300L))
        
        // then
        usage.cancelledAmount shouldBe Money.of(300L)
        usage.status shouldBe PointUsageStatus.PARTIALLY_CANCELLED
    }
    
    "전체 사용 금액 취소 시 상태가 FULLY_CANCELLED로 변경되어야 한다" {
        // given
        val usage = PointUsage(
            pointKey = "USAGE1234",
            memberId = 1L,
            orderNumber = OrderNumber.of("ORDER1234"),
            totalAmount = Money.of(1000L)
        )
        
        // when
        usage.cancel(Money.of(1000L))
        
        // then
        usage.cancelledAmount shouldBe Money.of(1000L)
        usage.status shouldBe PointUsageStatus.FULLY_CANCELLED
    }
    
    "부분 취소 시 상태가 PARTIALLY_CANCELLED로 변경되어야 한다" {
        // given
        val usage = PointUsage(
            pointKey = "USAGE1234",
            memberId = 1L,
            orderNumber = OrderNumber.of("ORDER1234"),
            totalAmount = Money.of(1000L)
        )
        
        // when
        usage.cancel(Money.of(500L))
        
        // then
        usage.status shouldBe PointUsageStatus.PARTIALLY_CANCELLED
    }
    
    "취소할 금액이 남은 금액보다 크면 예외가 발생해야 한다" {
        // given
        val usage = PointUsage(
            pointKey = "USAGE1234",
            memberId = 1L,
            orderNumber = OrderNumber.of("ORDER1234"),
            totalAmount = Money.of(1000L)
        )
        usage.cancel(Money.of(500L))
        
        // when & then
        shouldThrow<CannotCancelUsageException> {
            usage.cancel(Money.of(600L))
        }
    }
    
    "취소 금액이 총 사용 금액을 초과하면 생성 시 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointUsage(
                pointKey = "USAGE1234",
                memberId = 1L,
                orderNumber = OrderNumber.of("ORDER1234"),
                totalAmount = Money.of(1000L),
                cancelledAmount = Money.of(1500L)
            )
        }
    }
})

