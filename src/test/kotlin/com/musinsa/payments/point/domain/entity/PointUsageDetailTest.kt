package com.musinsa.payments.point.domain.entity

import com.musinsa.payments.point.domain.exception.CannotCancelDetailException
import com.musinsa.payments.point.domain.valueobject.Money
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PointUsageDetailTest : StringSpec({
    
    "PointUsageDetail를 생성할 수 있어야 한다" {
        // given
        val pointUsageId = 1L
        val pointAccumulationId = 2L
        val amount = Money.of(1000L)
        
        // when
        val detail = PointUsageDetail(
            pointUsageId = pointUsageId,
            pointAccumulationId = pointAccumulationId,
            amount = amount
        )
        
        // then
        detail.pointUsageId shouldBe pointUsageId
        detail.pointAccumulationId shouldBe pointAccumulationId
        detail.amount shouldBe amount
        detail.cancelledAmount shouldBe Money.ZERO
    }
    
    "남은 금액을 계산할 수 있어야 한다" {
        // given
        val detail = PointUsageDetail(
            pointUsageId = 1L,
            pointAccumulationId = 2L,
            amount = Money.of(1000L)
        )
        detail.cancel(Money.of(300L))
        
        // when
        val remaining = detail.getRemainingAmount()
        
        // then
        remaining shouldBe Money.of(700L)
    }
    
    "상세 내역 취소가 정상적으로 동작해야 한다" {
        // given
        val detail = PointUsageDetail(
            pointUsageId = 1L,
            pointAccumulationId = 2L,
            amount = Money.of(1000L)
        )
        
        // when
        detail.cancel(Money.of(300L))
        
        // then
        detail.cancelledAmount shouldBe Money.of(300L)
    }
    
    "전체 취소 여부를 확인할 수 있어야 한다" {
        // given
        val detail = PointUsageDetail(
            pointUsageId = 1L,
            pointAccumulationId = 2L,
            amount = Money.of(1000L)
        )
        detail.cancel(Money.of(1000L))
        
        // when
        val result = detail.isFullyCancelled()
        
        // then
        result shouldBe true
    }
    
    "부분 취소 시 전체 취소가 아니어야 한다" {
        // given
        val detail = PointUsageDetail(
            pointUsageId = 1L,
            pointAccumulationId = 2L,
            amount = Money.of(1000L)
        )
        detail.cancel(Money.of(500L))
        
        // when
        val result = detail.isFullyCancelled()
        
        // then
        result shouldBe false
    }
    
    "취소할 금액이 남은 금액보다 크면 예외가 발생해야 한다" {
        // given
        val detail = PointUsageDetail(
            pointUsageId = 1L,
            pointAccumulationId = 2L,
            amount = Money.of(1000L)
        )
        detail.cancel(Money.of(500L))
        
        // when & then
        shouldThrow<CannotCancelDetailException> {
            detail.cancel(Money.of(600L))
        }
    }
    
    "포인트 사용 ID가 0 이하면 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointUsageDetail(
                pointUsageId = 0L,
                pointAccumulationId = 2L,
                amount = Money.of(1000L)
            )
        }
    }
    
    "포인트 적립 ID가 0 이하면 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointUsageDetail(
                pointUsageId = 1L,
                pointAccumulationId = 0L,
                amount = Money.of(1000L)
            )
        }
    }
    
    "사용 금액이 0원 이하면 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointUsageDetail(
                pointUsageId = 1L,
                pointAccumulationId = 2L,
                amount = Money.ZERO
            )
        }
    }
    
    "취소 금액이 사용 금액을 초과하면 생성 시 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointUsageDetail(
                pointUsageId = 1L,
                pointAccumulationId = 2L,
                amount = Money.of(1000L),
                cancelledAmount = Money.of(1500L)
            )
        }
    }
})

