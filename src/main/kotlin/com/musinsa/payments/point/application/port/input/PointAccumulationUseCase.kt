package com.musinsa.payments.point.application.port.input

import com.musinsa.payments.point.domain.entity.PointAccumulation

/**
 * 포인트 적립 Use Case 인터페이스
 * Application 레이어에서 정의하는 인바운드 포트입니다.
 * Presentation 레이어에서 이 인터페이스를 호출합니다.
 */
interface PointAccumulationUseCase {

    /**
     * 포인트 적립
     * @param memberId 회원 ID
     * @param amount 적립 금액
     * @param expirationDays 만료일 (일 단위, null이면 기본값 사용)
     * @param isManualGrant 수기 지급 여부
     * @return 적립된 포인트 적립 엔티티
     */
    suspend fun accumulate(
        memberId: Long,
        amount: Long,
        expirationDays: Int? = null,
        isManualGrant: Boolean = false
    ): PointAccumulation

    /**
     * 포인트 적립 취소
     * @param pointKey 취소할 적립 건의 포인트 키
     * @param reason 취소 사유 (옵션)
     * @return 취소된 포인트 적립 엔티티
     */
    suspend fun cancelAccumulation(
        pointKey: String,
        reason: String? = null
    ): PointAccumulation
}

