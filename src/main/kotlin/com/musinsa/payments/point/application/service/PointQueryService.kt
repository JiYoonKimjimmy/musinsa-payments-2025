package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.input.PointBalanceResult
import com.musinsa.payments.point.application.port.input.PointQueryUseCase
import com.musinsa.payments.point.application.port.output.persistence.MemberPointBalancePersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.domain.entity.PointUsage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 조회 서비스
 * PointQueryUseCase 인터페이스를 구현합니다.
 * 
 * 잔액 조회 전략 (하이브리드):
 * - 빠른 조회: MemberPointBalance 테이블 우선 조회 (O(1))
 * - Fallback: 캐시가 없는 경우 SUM 쿼리로 계산
 */
@Transactional(readOnly = true)
@Service
class PointQueryService(
    private val pointAccumulationPersistencePort: PointAccumulationPersistencePort,
    private val pointUsagePersistencePort: PointUsagePersistencePort,
    private val memberPointBalancePersistencePort: MemberPointBalancePersistencePort
) : PointQueryUseCase {
    
    override fun getBalance(memberId: Long): PointBalanceResult {
        // 적립 내역 조회 (모든 상태)
        val allAccumulations = pointAccumulationPersistencePort.findByMemberIdAndStatus(memberId, PointAccumulationStatus.ACCUMULATED)
        
        // 총 잔액 계산 (모든 적립 금액 합계)
        val totalBalance = allAccumulations.sumOf { it.amount.toLong() }
        
        // 사용 가능 잔액 계산: 캐시된 잔액 우선 조회, 없으면 SUM 계산
        val availableBalance = getAvailableBalance(memberId, allAccumulations)
        
        // 만료 잔액 계산 (만료된 적립 건의 사용 가능 잔액 합계)
        val expiredBalance = allAccumulations
            .filter { it.isExpired() }
            .sumOf { it.availableAmount.toLong() }
        
        return PointBalanceResult(
            memberId = memberId,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            expiredBalance = expiredBalance,
            accumulations = allAccumulations
        )
    }
    
    /**
     * 사용 가능 잔액 조회
     * MemberPointBalance 테이블을 우선 조회하고, 없으면 적립 건에서 계산합니다.
     */
    private fun getAvailableBalance(
        memberId: Long,
        accumulations: List<PointAccumulation>
    ): Long {
        // 캐시된 잔액 우선 조회 (O(1))
        val cachedBalance = memberPointBalancePersistencePort.findByMemberId(memberId)
        
        return if (cachedBalance.isPresent) {
            cachedBalance.get().availableBalance.toLong()
        } else {
            // Fallback: 적립 건에서 직접 계산
            accumulations
                .filter { !it.isExpired() }
                .sumOf { it.availableAmount.toLong() }
        }
    }
    
    override fun getUsageHistory(
        memberId: Long,
        orderNumber: String?,
        pageable: Pageable
    ): Page<PointUsage> {
        return pointUsagePersistencePort.findUsageHistoryByMemberId(
            memberId = memberId,
            orderNumber = orderNumber,
            pageable = pageable
        )
    }
}

