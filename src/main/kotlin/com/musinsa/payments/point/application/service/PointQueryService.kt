package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.input.PointQueryUseCase
import com.musinsa.payments.point.application.port.input.PointBalanceResult
import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 조회 서비스
 * PointQueryUseCase 인터페이스를 구현합니다.
 */
@Transactional(readOnly = true)
@Service
class PointQueryService(
    private val pointAccumulationPersistencePort: PointAccumulationPersistencePort,
    private val pointUsagePersistencePort: PointUsagePersistencePort
) : PointQueryUseCase {
    
    override fun getBalance(memberId: Long): PointBalanceResult {
        // 적립 내역 조회 (모든 상태)
        val allAccumulations = pointAccumulationPersistencePort
            .findByMemberIdAndStatus(memberId, PointAccumulationStatus.ACCUMULATED)
        
        // 총 잔액 계산 (모든 적립 금액 합계)
        val totalBalance = allAccumulations
            .sumOf { it.amount.toLong() }
        
        // 사용 가능 잔액 계산 (사용 가능한 적립 건의 사용 가능 잔액 합계)
        val availableBalance = allAccumulations
            .filter { !it.isExpired() }
            .sumOf { it.availableAmount.toLong() }
        
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
    
    override fun getUsageHistory(
        memberId: Long,
        orderNumber: String?,
        pageable: Pageable
    ): Page<com.musinsa.payments.point.domain.entity.PointUsage> {
        return pointUsagePersistencePort.findUsageHistoryByMemberId(
            memberId = memberId,
            orderNumber = orderNumber,
            pageable = pageable
        )
    }
}

