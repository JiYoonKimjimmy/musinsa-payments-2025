package com.musinsa.payments.point.domain.entity

import com.musinsa.payments.point.domain.valueobject.Money
import java.time.LocalDateTime

/**
 * 회원 포인트 잔액 도메인 엔티티
 * 회원별 포인트 잔액을 관리합니다.
 */
class MemberPointBalance(
    val memberId: Long,
    availableBalance: Money = Money.ZERO,
    totalAccumulated: Money = Money.ZERO,
    totalUsed: Money = Money.ZERO,
    totalExpired: Money = Money.ZERO,
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) {
    var availableBalance: Money = availableBalance
        private set
    
    var totalAccumulated: Money = totalAccumulated
        private set
    
    var totalUsed: Money = totalUsed
        private set
    
    var totalExpired: Money = totalExpired
        private set
    
    var createdAt: LocalDateTime = createdAt
        private set
    
    var updatedAt: LocalDateTime = updatedAt
        private set
    
    var version: Long = 0
    
    /**
     * 포인트 적립 처리
     * 사용 가능 잔액과 총 적립액을 증가시킵니다.
     */
    fun addBalance(amount: Money) {
        require(amount.isGreaterThan(Money.ZERO)) { "적립 금액은 0보다 커야 합니다." }
        availableBalance = availableBalance.add(amount)
        totalAccumulated = totalAccumulated.add(amount)
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * 포인트 사용 처리
     * 사용 가능 잔액을 감소시키고 총 사용액을 증가시킵니다.
     * 
     * 참고: 이 엔티티는 캐시 테이블로, 비동기 이벤트 처리 순서가 보장되지 않습니다.
     * 따라서 잔액 부족 검증은 하지 않으며, 음수가 될 수 있습니다.
     * 실제 잔액 검증은 서비스 레이어에서 수행되며, 
     * 캐시 불일치는 reconciliation 서비스에서 주기적으로 보정됩니다.
     */
    fun subtractBalance(amount: Money) {
        require(amount.isGreaterThan(Money.ZERO)) { "사용 금액은 0보다 커야 합니다." }
        availableBalance = availableBalance.subtract(amount)
        totalUsed = totalUsed.add(amount)
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * 포인트 복원 처리 (사용 취소)
     * 사용 가능 잔액을 증가시킵니다.
     */
    fun restoreBalance(amount: Money) {
        require(amount.isGreaterThan(Money.ZERO)) { "복원 금액은 0보다 커야 합니다." }
        availableBalance = availableBalance.add(amount)
        // 사용 취소 시 totalUsed는 감소시키지 않음 (이력 보존)
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * 적립 취소 처리
     * 사용 가능 잔액을 감소시킵니다.
     * 
     * 참고: 이 엔티티는 캐시 테이블로, 비동기 이벤트 처리 순서가 보장되지 않습니다.
     * 따라서 잔액 부족 검증은 하지 않으며, 음수가 될 수 있습니다.
     */
    fun cancelAccumulation(amount: Money) {
        require(amount.isGreaterThan(Money.ZERO)) { "취소 금액은 0보다 커야 합니다." }
        availableBalance = availableBalance.subtract(amount)
        // 적립 취소 시 totalAccumulated는 감소시키지 않음 (이력 보존)
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * 포인트 만료 처리
     * 사용 가능 잔액을 감소시키고 총 만료액을 증가시킵니다.
     * 
     * 참고: 이 엔티티는 캐시 테이블로, 비동기 이벤트 처리 순서가 보장되지 않습니다.
     * 따라서 잔액 부족 검증은 하지 않으며, 음수가 될 수 있습니다.
     */
    fun expireBalance(amount: Money) {
        require(amount.isGreaterThan(Money.ZERO)) { "만료 금액은 0보다 커야 합니다." }
        availableBalance = availableBalance.subtract(amount)
        totalExpired = totalExpired.add(amount)
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * 잔액 보정 처리
     * 정합성 검증 후 실제 잔액으로 보정합니다.
     */
    fun reconcile(actualBalance: Money) {
        availableBalance = actualBalance
        updatedAt = LocalDateTime.now()
    }
    
    companion object {
        /**
         * 기존 데이터로부터 복원
         */
        fun restore(
            memberId: Long,
            availableBalance: Money,
            totalAccumulated: Money,
            totalUsed: Money,
            totalExpired: Money,
            version: Long,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ): MemberPointBalance {
            val balance = MemberPointBalance(
                memberId = memberId,
                availableBalance = availableBalance,
                totalAccumulated = totalAccumulated,
                totalUsed = totalUsed,
                totalExpired = totalExpired,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
            balance.version = version
            return balance
        }
    }
}

