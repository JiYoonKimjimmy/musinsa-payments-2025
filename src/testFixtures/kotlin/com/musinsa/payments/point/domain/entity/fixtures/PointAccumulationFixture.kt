package com.musinsa.payments.point.domain.entity.fixtures

import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.valueobject.Money
import java.time.LocalDate

/**
 * PointAccumulation 테스트 Fixture
 * 테스트에서 PointAccumulation 도메인 엔티티를 쉽게 생성하기 위한 헬퍼 객체입니다.
 */
object PointAccumulationFixture {

    /**
     * 기본 포인트 적립 생성
     * 전액 사용 가능한 일반 적립 건을 생성합니다.
     */
    fun create(
        pointKey: String = "TEST_KEY",
        memberId: Long = 1L,
        amount: Long = 10000L,
        expirationDate: LocalDate = LocalDate.now().plusDays(365),
        isManualGrant: Boolean = false
    ): PointAccumulation {
        return PointAccumulation(
            pointKey = pointKey,
            memberId = memberId,
            amount = Money.of(amount),
            expirationDate = expirationDate,
            isManualGrant = isManualGrant
        )
    }

    /**
     * 일부 사용된 포인트 적립 생성
     * availableAmount가 amount보다 작은 적립 건을 생성합니다.
     */
    fun createPartiallyUsed(
        pointKey: String = "TEST_KEY",
        memberId: Long = 1L,
        amount: Long = 10000L,
        availableAmount: Long = 5000L,
        expirationDate: LocalDate = LocalDate.now().plusDays(365),
        isManualGrant: Boolean = false
    ): PointAccumulation {
        require(availableAmount <= amount) {
            "사용 가능한 금액은 적립 금액보다 클 수 없습니다."
        }

        val accumulation = PointAccumulation(
            pointKey = pointKey,
            memberId = memberId,
            amount = Money.of(amount),
            expirationDate = expirationDate,
            isManualGrant = isManualGrant
        )
        accumulation.availableAmount = Money.of(availableAmount)
        return accumulation
    }

    /**
     * 만료된 포인트 적립 생성
     * expirationDate가 과거인 적립 건을 생성합니다.
     *
     * 주의: PointAccumulation 생성자는 만료일 검증을 수행하므로,
     * 일단 유효한 날짜로 생성한 후 expirationDate를 과거로 변경합니다.
     */
    fun createExpired(
        pointKey: String = "EXPIRED_KEY",
        memberId: Long = 1L,
        amount: Long = 5000L,
        availableAmount: Long = amount,
        daysAgo: Long = 1L,
        isManualGrant: Boolean = false
    ): PointAccumulation {
        require(availableAmount <= amount) {
            "사용 가능한 금액은 적립 금액보다 클 수 없습니다."
        }

        // 일단 유효한 날짜로 생성
        val accumulation = PointAccumulation(
            pointKey = pointKey,
            memberId = memberId,
            amount = Money.of(amount),
            expirationDate = LocalDate.now().plusDays(1),
            isManualGrant = isManualGrant
        )

        // availableAmount 설정
        accumulation.availableAmount = Money.of(availableAmount)

        // 만료일을 과거로 변경
        accumulation.expirationDate = LocalDate.now().minusDays(daysAgo)

        return accumulation
    }

    /**
     * 수기 지급 포인트 적립 생성
     * isManualGrant가 true인 적립 건을 생성합니다.
     */
    fun createManualGrant(
        pointKey: String = "MANUAL_KEY",
        memberId: Long = 1L,
        amount: Long = 5000L,
        expirationDate: LocalDate = LocalDate.now().plusDays(365)
    ): PointAccumulation {
        return create(
            pointKey = pointKey,
            memberId = memberId,
            amount = amount,
            expirationDate = expirationDate,
            isManualGrant = true
        )
    }

    /**
     * 전액 소진된 포인트 적립 생성
     * availableAmount가 0인 적립 건을 생성합니다.
     */
    fun createFullyUsed(
        pointKey: String = "FULLY_USED_KEY",
        memberId: Long = 1L,
        amount: Long = 10000L,
        expirationDate: LocalDate = LocalDate.now().plusDays(365),
        isManualGrant: Boolean = false
    ): PointAccumulation {
        return createPartiallyUsed(
            pointKey = pointKey,
            memberId = memberId,
            amount = amount,
            availableAmount = 0L,
            expirationDate = expirationDate,
            isManualGrant = isManualGrant
        )
    }

    /**
     * 곧 만료될 포인트 적립 생성
     * 지정한 일수 후에 만료되는 적립 건을 생성합니다.
     */
    fun createExpiringSoon(
        pointKey: String = "EXPIRING_SOON_KEY",
        memberId: Long = 1L,
        amount: Long = 10000L,
        availableAmount: Long = amount,
        daysUntilExpiration: Long = 7L,
        isManualGrant: Boolean = false
    ): PointAccumulation {
        return createPartiallyUsed(
            pointKey = pointKey,
            memberId = memberId,
            amount = amount,
            availableAmount = availableAmount,
            expirationDate = LocalDate.now().plusDays(daysUntilExpiration),
            isManualGrant = isManualGrant
        )
    }
}
