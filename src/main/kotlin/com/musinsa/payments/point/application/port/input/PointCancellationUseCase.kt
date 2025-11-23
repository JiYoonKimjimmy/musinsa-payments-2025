package com.musinsa.payments.point.application.port.input

import com.musinsa.payments.point.domain.entity.PointUsage

/**
 * 포인트 취소 Use Case 인터페이스
 * Application 레이어에서 정의하는 인바운드 포트입니다.
 * Presentation 레이어에서 이 인터페이스를 호출합니다.
 */
interface PointCancellationUseCase {
    
    /**
     * 포인트 사용 취소
     * @param pointKey 취소할 사용 건의 포인트 키
     * @param amount 취소할 금액 (null이면 전체 취소)
     * @param reason 취소 사유 (옵션)
     * @return 취소된 포인트 사용 엔티티
     */
    fun cancelUsage(
        pointKey: String,
        amount: Long? = null,
        reason: String? = null
    ): PointUsage
}

