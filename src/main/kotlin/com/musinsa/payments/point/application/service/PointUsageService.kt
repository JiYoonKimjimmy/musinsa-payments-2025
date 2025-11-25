package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.input.PointUsageUseCase
import com.musinsa.payments.point.application.port.output.PointKeyGenerator
import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsageDetailPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.entity.PointUsageDetail
import com.musinsa.payments.point.domain.service.PointUsagePriorityService
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 사용 서비스
 * PointUsageUseCase 인터페이스를 구현합니다.
 */
@Transactional(isolation = Isolation.READ_COMMITTED)
@Service
class PointUsageService(
    private val pointAccumulationPersistencePort: PointAccumulationPersistencePort,
    private val pointUsagePersistencePort: PointUsagePersistencePort,
    private val pointUsageDetailPersistencePort: PointUsageDetailPersistencePort,
    private val pointKeyGenerator: PointKeyGenerator,
    private val pointUsagePriorityService: PointUsagePriorityService
) : PointUsageUseCase {
    
    override fun use(
        memberId: Long,
        orderNumber: String,
        amount: Long
    ): PointUsage {
        // 사용 금액 검증
        val usageAmount = Money.of(amount)
        if (usageAmount.isLessThanOrEqual(Money.ZERO)) {
            throw IllegalArgumentException("사용 금액은 0보다 커야 합니다.")
        }
        
        // 사용 가능 잔액 확인
        val availableBalance = pointAccumulationPersistencePort
            .sumAvailableAmountByMemberId(memberId)
        
        if (availableBalance.isLessThan(usageAmount)) {
            throw com.musinsa.payments.point.domain.exception.InsufficientPointException()
        }
        
        // 사용 가능한 적립 건 조회 (비관적 락 적용)
        val availableAccumulations = pointAccumulationPersistencePort
            .findAvailableAccumulationsByMemberIdWithLock(memberId)
        
        // 포인트 사용 우선순위에 따라 적립 건 선택
        val selectedAccumulations = pointUsagePriorityService.selectAccumulationsForUsage(
            memberId = memberId,
            usageAmount = usageAmount,
            accumulations = availableAccumulations
        )
        
        // 포인트 키 생성
        val pointKey = pointKeyGenerator.generate().value
        
        // 포인트 사용 엔티티 생성
        val usage = PointUsage(
            pointKey = pointKey,
            memberId = memberId,
            orderNumber = OrderNumber.of(orderNumber),
            totalAmount = usageAmount
        )
        
        // 포인트 사용 엔티티 먼저 저장 (ID 생성)
        val savedUsage = pointUsagePersistencePort.save(usage)
        val usageId = savedUsage.id
            ?: throw IllegalStateException("사용 건 ID가 없습니다.")
        
        // 포인트 사용 처리 및 상세 내역 생성
        val usageDetails = mutableListOf<PointUsageDetail>()
        var remainingAmount = usageAmount
        
        for (accumulation in selectedAccumulations) {
            val available = accumulation.availableAmount
            val useAmount = if (remainingAmount.isLessThan(available)) {
                remainingAmount
            } else {
                available
            }
            
            // 적립 건에서 포인트 사용 처리
            accumulation.use(useAmount)
            
            // 1원 단위로 상세 내역 생성
            val details = createUsageDetails(
                pointUsageId = usageId,
                pointAccumulationId = accumulation.id
                    ?: throw IllegalStateException("적립 건 ID가 없습니다."),
                amount = useAmount
            )
            usageDetails.addAll(details)
            
            remainingAmount = remainingAmount.subtract(useAmount)
            
            // 적립 건 저장 (사용 가능 잔액 업데이트)
            pointAccumulationPersistencePort.save(accumulation)
        }
        
        // 상세 내역 저장
        pointUsageDetailPersistencePort.saveAll(usageDetails)
        
        return savedUsage
    }
    
    /**
     * 1원 단위로 사용 상세 내역 생성
     */
    private fun createUsageDetails(
        pointUsageId: Long,
        pointAccumulationId: Long,
        amount: Money
    ): List<PointUsageDetail> {
        val details = mutableListOf<PointUsageDetail>()
        val amountLong = amount.toLong()
        
        // 1원 단위로 상세 내역 생성
        for (i in 0 until amountLong) {
            details.add(
                PointUsageDetail(
                    pointUsageId = pointUsageId,
                    pointAccumulationId = pointAccumulationId,
                    amount = Money.of(1L)
                )
            )
        }
        
        return details
    }
}

