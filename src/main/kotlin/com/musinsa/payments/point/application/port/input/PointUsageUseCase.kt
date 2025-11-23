package com.musinsa.payments.point.application.port.input

import com.musinsa.payments.point.domain.entity.PointUsage

/**
 * 포인트 사용 Use Case 인터페이스
 * Application 레이어에서 정의하는 인바운드 포트입니다.
 * Presentation 레이어에서 이 인터페이스를 호출합니다.
 */
interface PointUsageUseCase {
    
    /**
     * 포인트 사용
     * @param memberId 회원 ID
     * @param orderNumber 주문번호
     * @param amount 사용 금액
     * @return 사용된 포인트 사용 엔티티
     */
    fun use(
        memberId: Long,
        orderNumber: String,
        amount: Long
    ): PointUsage
}

