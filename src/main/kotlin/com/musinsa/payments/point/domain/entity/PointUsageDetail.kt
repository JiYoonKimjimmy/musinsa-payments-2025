package com.musinsa.payments.point.domain.entity

import com.musinsa.payments.point.domain.exception.CannotCancelDetailException
import com.musinsa.payments.point.domain.valueobject.Money
import java.time.LocalDateTime

/**
 * 포인트 사용 상세 도메인 엔티티
 * 1원 단위로 포인트 사용을 추적합니다.
 * 어떤 적립 건에서 얼마를 사용했는지 상세히 기록합니다.
 */
class PointUsageDetail {
    var id: Long? = null              // 엔티티 생성 시점에는 null, 저장 후에는 항상 값 존재
    var pointUsageId: Long            // 포인트 사용 ID, 필수
    var pointAccumulationId: Long     // 포인트 적립 ID, 필수
    var amount: Money                 // 사용 금액 (1원 단위), 필수
    var cancelledAmount: Money        // 취소된 금액, 필수 (기본값 Money.ZERO)
    var createdAt: LocalDateTime      // 생성일시, 필수
    var updatedAt: LocalDateTime      // 수정일시, 필수
    
    constructor(
        pointUsageId: Long,
        pointAccumulationId: Long,
        amount: Money,
        cancelledAmount: Money = Money.ZERO,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ) {
        require(pointUsageId > 0) { "포인트 사용 ID는 0보다 커야 합니다." }
        require(pointAccumulationId > 0) { "포인트 적립 ID는 0보다 커야 합니다." }
        require(amount.isGreaterThan(Money.ZERO)) { "사용 금액은 0보다 커야 합니다." }
        require(cancelledAmount.isGreaterThanOrEqual(Money.ZERO)) { "취소 금액은 0 이상이어야 합니다." }
        require(cancelledAmount.isLessThanOrEqual(amount)) { "취소 금액은 사용 금액을 초과할 수 없습니다." }
        
        this.pointUsageId = pointUsageId
        this.pointAccumulationId = pointAccumulationId
        this.amount = amount
        this.cancelledAmount = cancelledAmount
        this.createdAt = createdAt
        this.updatedAt = updatedAt
    }
    
    /**
     * 남은 금액 계산
     * 사용 금액에서 취소된 금액을 뺀 값입니다.
     */
    fun getRemainingAmount(): Money {
        return amount.subtract(cancelledAmount)
    }
    
    /**
     * 상세 내역 취소 처리
     * 취소 금액을 증가시킵니다.
     */
    fun cancel(cancelAmount: Money) {
        if (getRemainingAmount().isLessThan(cancelAmount)) {
            throw CannotCancelDetailException()
        }
        cancelledAmount = cancelledAmount.add(cancelAmount)
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * 전체 취소 여부 확인
     * 취소된 금액이 사용 금액과 같으면 전체 취소입니다.
     */
    fun isFullyCancelled(): Boolean {
        return cancelledAmount == amount
    }
}
