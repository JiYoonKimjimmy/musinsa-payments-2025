package com.musinsa.payments.point.domain.entity.fixtures

import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.entity.PointUsageStatus
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber

/**
 * PointUsage 테스트 Fixture
 * 테스트에서 PointUsage 도메인 엔티티를 쉽게 생성하기 위한 헬퍼 객체입니다.
 */
object PointUsageFixture {

    /**
     * 기본 포인트 사용 생성
     * 취소되지 않은 일반 사용 건을 생성합니다.
     */
    fun create(
        pointKey: String = "USAGE_KEY",
        memberId: Long = 1L,
        orderNumber: String = "ORDER123",
        totalAmount: Long = 5000L,
        status: PointUsageStatus = PointUsageStatus.USED
    ): PointUsage {
        return PointUsage(
            pointKey = pointKey,
            memberId = memberId,
            orderNumber = OrderNumber.of(orderNumber),
            totalAmount = Money.of(totalAmount),
            status = status
        )
    }

    /**
     * 부분 취소된 포인트 사용 생성
     * 일부 금액이 취소된 사용 건을 생성합니다.
     */
    fun createPartiallyCancelled(
        pointKey: String = "USAGE_KEY",
        memberId: Long = 1L,
        orderNumber: String = "ORDER123",
        totalAmount: Long = 10000L,
        cancelledAmount: Long = 3000L,
        status: PointUsageStatus = PointUsageStatus.PARTIALLY_CANCELLED
    ): PointUsage {
        require(cancelledAmount <= totalAmount) {
            "취소 금액은 총 사용 금액을 초과할 수 없습니다."
        }

        return PointUsage(
            pointKey = pointKey,
            memberId = memberId,
            orderNumber = OrderNumber.of(orderNumber),
            totalAmount = Money.of(totalAmount),
            cancelledAmount = Money.of(cancelledAmount),
            status = status
        )
    }

    /**
     * 전체 취소된 포인트 사용 생성
     * 전액 취소된 사용 건을 생성합니다.
     */
    fun createFullyCancelled(
        pointKey: String = "USAGE_KEY",
        memberId: Long = 1L,
        orderNumber: String = "ORDER123",
        totalAmount: Long = 5000L
    ): PointUsage {
        return PointUsage(
            pointKey = pointKey,
            memberId = memberId,
            orderNumber = OrderNumber.of(orderNumber),
            totalAmount = Money.of(totalAmount),
            cancelledAmount = Money.of(totalAmount),
            status = PointUsageStatus.FULLY_CANCELLED
        )
    }

    /**
     * Money 타입으로 총 사용 금액을 받는 기본 포인트 사용 생성
     * 테스트에서 Money 타입을 직접 사용하는 경우를 위한 헬퍼 메서드입니다.
     */
    fun createWithMoney(
        pointKey: String = "USAGE_KEY",
        memberId: Long = 1L,
        orderNumber: String = "ORDER123",
        totalAmount: Money,
        status: PointUsageStatus = PointUsageStatus.USED
    ): PointUsage {
        return PointUsage(
            pointKey = pointKey,
            memberId = memberId,
            orderNumber = OrderNumber.of(orderNumber),
            totalAmount = totalAmount,
            status = status
        )
    }

    /**
     * 여러 개의 포인트 사용 건 생성
     * 순차적인 pointKey를 가진 여러 사용 건을 생성합니다.
     */
    fun createMultiple(
        basePointKey: String = "USAGE",
        memberId: Long = 1L,
        baseOrderNumber: String = "ORDER",
        count: Int = 3,
        amountPerUsage: Long = 1000L
    ): List<PointUsage> {
        return (1..count).map { index ->
            create(
                pointKey = "${basePointKey}${String.format("%02d", index)}",
                memberId = memberId,
                orderNumber = "$baseOrderNumber$index",
                totalAmount = amountPerUsage * index
            )
        }
    }
}
