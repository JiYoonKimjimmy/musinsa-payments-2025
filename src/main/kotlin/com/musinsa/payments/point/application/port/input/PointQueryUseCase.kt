package com.musinsa.payments.point.application.port.input

import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointUsage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 포인트 조회 Use Case 인터페이스
 * Application 레이어에서 정의하는 인바운드 포트입니다.
 * Presentation 레이어에서 이 인터페이스를 호출합니다.
 */
interface PointQueryUseCase {
    
    /**
     * 포인트 잔액 조회
     * @param memberId 회원 ID
     * @return 포인트 적립 엔티티 목록 (잔액 계산에 사용)
     */
    fun getBalance(memberId: Long): PointBalanceResult
    
    /**
     * 포인트 사용 내역 조회
     * @param memberId 회원 ID
     * @param orderNumber 주문번호 (옵션, null이면 전체 조회)
     * @param pageable 페이징 정보
     * @return 포인트 사용 엔티티 페이지
     */
    fun getUsageHistory(
        memberId: Long,
        orderNumber: String? = null,
        pageable: Pageable
    ): Page<PointUsage>
}

/**
 * 포인트 잔액 조회 결과
 */
data class PointBalanceResult(
    val memberId: Long,
    val totalBalance: Long,
    val availableBalance: Long,
    val expiredBalance: Long,
    val accumulations: List<PointAccumulation>
)

