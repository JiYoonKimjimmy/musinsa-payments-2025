package com.musinsa.payments.point.domain.service

import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.exception.InsufficientPointException
import com.musinsa.payments.point.domain.valueobject.Money

/**
 * 포인트 사용 우선순위 도메인 서비스
 * 포인트 사용 시 우선순위에 따라 적립 건을 선택합니다.
 */
class PointUsagePriorityService {
    
    /**
     * 포인트 사용을 위한 적립 건 선택
     * 
     * 우선순위:
     * 1. 수기 지급 포인트 우선
     * 2. 만료일이 짧은 순서 (FIFO)
     * 
     * @param memberId 사용자 ID
     * @param usageAmount 사용할 금액
     * @param accumulations 사용 가능한 적립 건 목록
     * @return 사용할 적립 건 목록
     * @throws InsufficientPointException 사용 가능한 포인트가 부족한 경우
     */
    fun selectAccumulationsForUsage(
        memberId: Long,
        usageAmount: Money,
        accumulations: List<PointAccumulation>
    ): List<PointAccumulation> {
        // 사용 가능한 적립 건만 필터링
        val available = accumulations
            .filter { it.hasAvailableAmount() }
            .filter { !it.isExpired() }
        
        // 우선순위 정렬: 수기 지급 > 만료일 짧은 순
        val comparator = compareByDescending<PointAccumulation> { it.isManualGrant }.thenBy { it.expirationDate }
        val sorted = available.sortedWith(comparator)
        
        // 사용할 적립 건 선택
        return selectAccumulations(sorted, usageAmount)
    }
    
    /**
     * 우선순위에 따라 적립 건 선택
     * 
     * @param accumulations 정렬된 적립 건 목록
     * @param usageAmount 사용할 금액
     * @return 사용할 적립 건 목록
     * @throws InsufficientPointException 사용 가능한 포인트가 부족한 경우
     */
    private fun selectAccumulations(
        accumulations: List<PointAccumulation>,
        usageAmount: Money
    ): List<PointAccumulation> {
        val selected = mutableListOf<PointAccumulation>()
        var remaining = usageAmount
        
        for (accumulation in accumulations) {
            if (remaining.isLessThanOrEqual(Money.ZERO)) {
                break
            }
            
            val available = accumulation.availableAmount
            val useAmount = if (remaining.isLessThan(available)) {
                remaining
            } else {
                available
            }

            selected.add(accumulation)
            remaining = remaining.subtract(useAmount)
        }
        
        if (remaining.isGreaterThan(Money.ZERO)) {
            throw InsufficientPointException()
        }
        
        return selected
    }
}
