package com.musinsa.payments.point.domain.entity

import com.musinsa.payments.point.domain.exception.CannotCancelUsageException
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import java.time.LocalDateTime

/**
 * 포인트 사용 도메인 엔티티
 * 포인트 사용 정보와 비즈니스 로직을 관리합니다.
 */
class PointUsage(
    var id: Long? = null,
    val pointKey: String,
    val memberId: Long,
    val orderNumber: OrderNumber,
    val totalAmount: Money,
    cancelledAmount: Money = Money.ZERO,
    status: PointUsageStatus = PointUsageStatus.USED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) {
    var cancelledAmount: Money = cancelledAmount
        private set
    var status: PointUsageStatus = status
        private set
    var updatedAt: LocalDateTime = updatedAt
        private set
    
    init {
        require(pointKey.isNotBlank()) { "포인트 키는 필수입니다." }
        require(memberId > 0) { "사용자 ID는 0보다 커야 합니다." }
        require(totalAmount.isGreaterThan(Money.ZERO)) { "총 사용 금액은 0보다 커야 합니다." }
        require(cancelledAmount.isGreaterThanOrEqual(Money.ZERO)) { "취소 금액은 0 이상이어야 합니다." }
        require(cancelledAmount.isLessThanOrEqual(totalAmount)) { "취소 금액은 총 사용 금액을 초과할 수 없습니다." }
    }
    
    /**
     * 남은 사용 금액 계산
     * 총 사용 금액에서 취소된 금액을 뺀 값입니다.
     */
    fun getRemainingAmount(): Money {
        return totalAmount.subtract(cancelledAmount)
    }
    
    /**
     * 취소 가능 여부 확인
     * 남은 사용 금액이 취소할 금액 이상이어야 합니다.
     */
    fun canCancel(cancelAmount: Money): Boolean {
        return getRemainingAmount().isGreaterThanOrEqual(cancelAmount)
    }
    
    /**
     * 사용 취소 처리
     * 취소 금액을 증가시키고 상태를 업데이트합니다.
     */
    fun cancel(cancelAmount: Money) {
        if (!canCancel(cancelAmount)) {
            throw CannotCancelUsageException()
        }
        cancelledAmount = cancelledAmount.add(cancelAmount)
        updateStatus()
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * 상태 업데이트
     * 취소 금액에 따라 상태를 자동으로 업데이트합니다.
     */
    private fun updateStatus() {
        when {
            cancelledAmount == totalAmount -> {
                status = PointUsageStatus.FULLY_CANCELLED
            }
            cancelledAmount.isGreaterThan(Money.ZERO) -> {
                status = PointUsageStatus.PARTIALLY_CANCELLED
            }
        }
    }
}
