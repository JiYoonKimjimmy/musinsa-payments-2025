package com.musinsa.payments.point.domain.entity.fixtures

import com.musinsa.payments.point.domain.entity.PointUsageDetail
import com.musinsa.payments.point.domain.valueobject.Money
import java.time.LocalDateTime

/**
 * PointUsageDetail 테스트 Fixture
 * 테스트에서 PointUsageDetail 도메인 엔티티를 쉽게 생성하기 위한 헬퍼 객체입니다.
 */
object PointUsageDetailFixture {

    /**
     * 기본 포인트 사용 상세 내역 생성
     * 취소되지 않은 일반 사용 상세 내역을 생성합니다.
     */
    fun create(
        pointUsageId: Long,
        pointAccumulationId: Long,
        amount: Money,
        cancelledAmount: Money = Money.ZERO,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): PointUsageDetail {
        return PointUsageDetail(
            pointUsageId = pointUsageId,
            pointAccumulationId = pointAccumulationId,
            amount = amount,
            cancelledAmount = cancelledAmount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * 취소된 금액이 있는 포인트 사용 상세 내역 생성
     * 취소된 금액을 지정할 수 있는 사용 상세 내역을 생성합니다.
     */
    fun createCancelled(
        pointUsageId: Long,
        pointAccumulationId: Long,
        amount: Money,
        cancelledAmount: Money,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): PointUsageDetail {
        require(cancelledAmount.isLessThanOrEqual(amount)) {
            "취소 금액은 사용 금액을 초과할 수 없습니다."
        }
        require(cancelledAmount.isGreaterThan(Money.ZERO)) {
            "취소 금액은 0보다 커야 합니다."
        }

        return PointUsageDetail(
            pointUsageId = pointUsageId,
            pointAccumulationId = pointAccumulationId,
            amount = amount,
            cancelledAmount = cancelledAmount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * 부분 취소된 포인트 사용 상세 내역 생성
     * 일부 금액이 취소된 사용 상세 내역을 생성합니다.
     */
    fun createPartiallyCancelled(
        pointUsageId: Long,
        pointAccumulationId: Long,
        amount: Money,
        cancelledAmount: Money,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): PointUsageDetail {
        require(cancelledAmount.isLessThan(amount)) {
            "부분 취소 금액은 사용 금액보다 작아야 합니다."
        }
        require(cancelledAmount.isGreaterThan(Money.ZERO)) {
            "취소 금액은 0보다 커야 합니다."
        }

        return createCancelled(
            pointUsageId = pointUsageId,
            pointAccumulationId = pointAccumulationId,
            amount = amount,
            cancelledAmount = cancelledAmount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * 전체 취소된 포인트 사용 상세 내역 생성
     * 전액 취소된 사용 상세 내역을 생성합니다.
     */
    fun createFullyCancelled(
        pointUsageId: Long,
        pointAccumulationId: Long,
        amount: Money,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): PointUsageDetail {
        return PointUsageDetail(
            pointUsageId = pointUsageId,
            pointAccumulationId = pointAccumulationId,
            amount = amount,
            cancelledAmount = amount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * 1원 단위로 여러 개의 포인트 사용 상세 내역 생성
     * 테스트에서 1원 단위로 상세 내역을 생성해야 할 때 사용합니다.
     * 각 상세 내역은 1원씩 사용한 것으로 생성됩니다.
     */
    fun createMultipleOneWon(
        pointUsageId: Long,
        pointAccumulationId: Long,
        count: Long,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): List<PointUsageDetail> {
        require(count > 0) {
            "생성할 개수는 0보다 커야 합니다."
        }

        return (1..count).map {
            PointUsageDetail(
                pointUsageId = pointUsageId,
                pointAccumulationId = pointAccumulationId,
                amount = Money.of(1L),
                cancelledAmount = Money.ZERO,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
}

