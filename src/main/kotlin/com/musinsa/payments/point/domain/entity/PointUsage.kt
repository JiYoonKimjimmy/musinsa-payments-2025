package com.musinsa.payments.point.domain.entity

import com.musinsa.payments.point.domain.exception.CannotCancelUsageException
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import java.time.LocalDateTime

/**
 * 포인트 사용 도메인 엔티티
 * 포인트 사용 정보와 비즈니스 로직을 관리합니다.
 */
class PointUsage {
    var id: Long? = null              // 엔티티 생성 시점에는 null, 저장 후에는 항상 값 존재
    var pointKey: String              // 비즈니스 식별자, 필수
    var memberId: Long                // 사용자 ID, 필수
    var orderNumber: OrderNumber      // 주문번호, 필수
    var totalAmount: Money            // 총 사용 금액, 필수
    var cancelledAmount: Money        // 취소된 금액, 필수 (기본값 Money.ZERO)
    var status: PointUsageStatus      // 상태, 필수
    var createdAt: LocalDateTime      // 생성일시, 필수
    var updatedAt: LocalDateTime      // 수정일시, 필수
    
    constructor(
        pointKey: String,
        memberId: Long,
        orderNumber: OrderNumber,
        totalAmount: Money,
        cancelledAmount: Money = Money.ZERO,
        status: PointUsageStatus = PointUsageStatus.USED,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ) {
        require(pointKey.isNotBlank()) { "포인트 키는 필수입니다." }
        require(memberId > 0) { "사용자 ID는 0보다 커야 합니다." }
        require(totalAmount.isGreaterThan(Money.ZERO)) { "총 사용 금액은 0보다 커야 합니다." }
        require(cancelledAmount.isGreaterThanOrEqual(Money.ZERO)) { "취소 금액은 0 이상이어야 합니다." }
        require(cancelledAmount.isLessThanOrEqual(totalAmount)) { "취소 금액은 총 사용 금액을 초과할 수 없습니다." }
        
        this.pointKey = pointKey
        this.memberId = memberId
        this.orderNumber = orderNumber
        this.totalAmount = totalAmount
        this.cancelledAmount = cancelledAmount
        this.status = status
        this.createdAt = createdAt
        this.updatedAt = updatedAt
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
