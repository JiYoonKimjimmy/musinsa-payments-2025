package com.musinsa.payments.point.domain.entity

import com.musinsa.payments.point.domain.exception.CannotCancelAccumulationException
import com.musinsa.payments.point.domain.exception.InsufficientPointException
import com.musinsa.payments.point.domain.exception.InvalidAmountException
import com.musinsa.payments.point.domain.valueobject.Money
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 포인트 적립 도메인 엔티티
 * 포인트 적립 정보와 비즈니스 로직을 관리합니다.
 */
class PointAccumulation(
    val pointKey: String,
    val memberId: Long,
    val amount: Money,
    expirationDate: LocalDate,
    isManualGrant: Boolean = false,
    status: PointAccumulationStatus = PointAccumulationStatus.ACCUMULATED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
    skipValidation: Boolean = false  // restore() 메서드에서 검증을 우회하기 위한 플래그
) {
    var id: Long? = null                      // 엔티티 생성 시점에는 null, 저장 후에는 항상 값 존재
    var availableAmount: Money = amount         // 사용 가능 잔액, 필수 (초기값은 적립 금액과 동일)
        private set
    var expirationDate: LocalDate = expirationDate
        private set
    var isManualGrant: Boolean = isManualGrant
        private set
    var status: PointAccumulationStatus = status
        private set
    var updatedAt: LocalDateTime = updatedAt
        private set
    
    init {
        if (!skipValidation) {
            require(pointKey.isNotBlank()) { "포인트 키는 필수입니다." }
            require(memberId > 0) { "사용자 ID는 0보다 커야 합니다." }
            require(amount.isGreaterThan(Money.ZERO)) { "적립 금액은 0보다 커야 합니다." }
            require(expirationDate.isAfter(LocalDate.now()) || expirationDate.isEqual(LocalDate.now())) {
                "만료일은 오늘 이후여야 합니다."
            }
        }
    }
    
    companion object {
        /**
         * 이미 저장된 엔티티를 복원하기 위한 팩토리 메서드
         * Entity Mapper에서 사용합니다.
         * 만료일 검증을 우회하기 위해 skipValidation = true를 사용합니다.
         */
        fun restore(
            id: Long?,
            pointKey: String,
            memberId: Long,
            amount: Money,
            availableAmount: Money,
            expirationDate: LocalDate,
            isManualGrant: Boolean,
            status: PointAccumulationStatus,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ): PointAccumulation {
            val accumulation = PointAccumulation(
                pointKey = pointKey,
                memberId = memberId,
                amount = amount,
                expirationDate = expirationDate,
                isManualGrant = isManualGrant,
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt,
                skipValidation = true  // 검증 우회
            )
            accumulation.id = id
            accumulation.availableAmount = availableAmount
            return accumulation
        }
    }
    
    /**
     * 취소 가능 여부 확인
     * 적립 상태이고 사용된 금액이 없어야 취소 가능합니다.
     */
    fun canBeCancelled(): Boolean {
        return status == PointAccumulationStatus.ACCUMULATED 
            && availableAmount == amount
    }
    
    /**
     * 만료 여부 확인 (현재 시점 기준)
     */
    fun isExpired(): Boolean {
        return LocalDate.now().isAfter(expirationDate)
    }
    
    /**
     * 만료 여부 확인 (특정 날짜 기준)
     */
    fun isExpiredAt(date: LocalDate): Boolean {
        return date.isAfter(expirationDate)
    }
    
    /**
     * 포인트 사용 처리
     * 사용 가능 잔액에서 지정된 금액만큼 차감합니다.
     */
    fun use(usageAmount: Money) {
        if (availableAmount.isLessThan(usageAmount)) {
            throw InsufficientPointException()
        }
        availableAmount = availableAmount.subtract(usageAmount)
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * 적립 취소 처리
     */
    fun cancel() {
        if (!canBeCancelled()) {
            throw CannotCancelAccumulationException()
        }
        status = PointAccumulationStatus.CANCELLED
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * 만료 처리
     */
    fun markAsExpired() {
        status = PointAccumulationStatus.EXPIRED
        updatedAt = LocalDateTime.now()
    }
    
    
    /**
     * 사용 가능 잔액 존재 여부 확인
     */
    fun hasAvailableAmount(): Boolean {
        return availableAmount.isGreaterThan(Money.ZERO)
    }

    /**
     * 포인트 복원 처리
     * 사용 취소 시 사용 가능 잔액을 복원합니다.
     */
    fun restore(restoreAmount: Money): PointAccumulation {
        if (restoreAmount.isLessThanOrEqual(Money.ZERO)) {
            throw InvalidAmountException("복원 금액은 0보다 커야 합니다.")
        }
        val newAvailable = availableAmount.add(restoreAmount)
        if (newAvailable.isGreaterThan(amount)) {
            throw InvalidAmountException("복원 후 사용 가능 잔액이 적립 금액을 초과할 수 없습니다.")
        }
        availableAmount = newAvailable
        updatedAt = LocalDateTime.now()
        return this
    }
}
