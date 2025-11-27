package com.musinsa.payments.point.domain.event

import com.musinsa.payments.point.domain.entity.MemberPointBalance
import com.musinsa.payments.point.domain.valueobject.Money
import java.time.LocalDateTime

/**
 * 포인트 잔액 변경 이벤트
 * 포인트 거래(적립, 사용, 취소) 발생 시 잔액 업데이트를 위한 도메인 이벤트입니다.
 */
sealed class PointBalanceEvent(
    open val memberId: Long,
    open val amount: Money,
    open val pointKey: String,
    open val occurredAt: LocalDateTime = LocalDateTime.now()
) {
    /** 이벤트 타입명 (로깅용) */
    abstract val eventTypeName: String
    
    /** 추가 로그 정보 (옵션) */
    open val additionalLogInfo: String = ""
    
    /** 잔액 업데이트 액션 - 각 이벤트가 자신의 행동을 캡슐화 */
    abstract fun action(balance: MemberPointBalance)

    /**
     * 포인트 적립 이벤트
     */
    data class Accumulated(
        override val memberId: Long,
        override val amount: Money,
        override val pointKey: String,
        override val occurredAt: LocalDateTime = LocalDateTime.now()
    ) : PointBalanceEvent(memberId, amount, pointKey, occurredAt) {
        override val eventTypeName = "적립"
        override fun action(balance: MemberPointBalance) = balance.addBalance(amount)
    }
    
    /**
     * 포인트 적립 취소 이벤트
     */
    data class AccumulationCancelled(
        override val memberId: Long,
        override val amount: Money,
        override val pointKey: String,
        override val occurredAt: LocalDateTime = LocalDateTime.now()
    ) : PointBalanceEvent(memberId, amount, pointKey, occurredAt) {
        override val eventTypeName = "적립 취소"
        override fun action(balance: MemberPointBalance) = balance.cancelAccumulation(amount)
    }
    
    /**
     * 포인트 사용 이벤트
     */
    data class Used(
        override val memberId: Long,
        override val amount: Money,
        override val pointKey: String,
        val orderNumber: String,
        override val occurredAt: LocalDateTime = LocalDateTime.now()
    ) : PointBalanceEvent(memberId, amount, pointKey, occurredAt) {
        override val eventTypeName = "사용"
        override val additionalLogInfo = ", orderNumber=$orderNumber"
        override fun action(balance: MemberPointBalance) = balance.subtractBalance(amount)
    }
    
    /**
     * 포인트 사용 취소 이벤트
     */
    data class UsageCancelled(
        override val memberId: Long,
        override val amount: Money,
        override val pointKey: String,
        override val occurredAt: LocalDateTime = LocalDateTime.now()
    ) : PointBalanceEvent(memberId, amount, pointKey, occurredAt) {
        override val eventTypeName = "사용 취소"
        override fun action(balance: MemberPointBalance) = balance.restoreBalance(amount)
    }
    
    /**
     * 포인트 만료 이벤트
     */
    data class Expired(
        override val memberId: Long,
        override val amount: Money,
        override val pointKey: String,
        override val occurredAt: LocalDateTime = LocalDateTime.now()
    ) : PointBalanceEvent(memberId, amount, pointKey, occurredAt) {
        override val eventTypeName = "만료"
        override fun action(balance: MemberPointBalance) = balance.expireBalance(amount)
    }
}
