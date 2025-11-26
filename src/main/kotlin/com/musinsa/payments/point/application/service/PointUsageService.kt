package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.input.PointUsageUseCase
import com.musinsa.payments.point.application.port.output.PointKeyGenerator
import com.musinsa.payments.point.application.port.output.event.PointBalanceEventPublisher
import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsageDetailPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.entity.PointUsageDetail
import com.musinsa.payments.point.domain.event.PointBalanceEvent
import com.musinsa.payments.point.domain.exception.InsufficientPointException
import com.musinsa.payments.point.domain.service.PointUsagePriorityService
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 사용 서비스: PointUsageUseCase 인터페이스 구현체
 * 
 * 트랜잭션 격리 수준: `READ_COMMITTED`
 * - 포인트 사용은 동시성 제어가 중요한 작업
 * - 비관적 락을 통해 적립 건의 동시 접근 제어 구현
 * - `READ_COMMITTED` 적용하여 `Dirty-Read` 를 방지하면서도, 과도한 격리로 인한 성능 저하 방지
 */
@Transactional(isolation = Isolation.READ_COMMITTED)
@Service
class PointUsageService(
    private val pointAccumulationPersistencePort: PointAccumulationPersistencePort,
    private val pointUsagePersistencePort: PointUsagePersistencePort,
    private val pointUsageDetailPersistencePort: PointUsageDetailPersistencePort,
    private val pointKeyGenerator: PointKeyGenerator,
    private val pointUsagePriorityService: PointUsagePriorityService,
    private val pointBalanceEventPublisher: PointBalanceEventPublisher
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
        val availableBalance = pointAccumulationPersistencePort.sumAvailableAmountByMemberId(memberId)
        if (availableBalance.isLessThan(usageAmount)) {
            throw InsufficientPointException()
        }
        
        // 사용 가능한 적립 건 조회 (비관적 락 적용)
        val availableAccumulations = pointAccumulationPersistencePort.findAvailableAccumulationsByMemberIdWithLock(memberId)
        
        // 포인트 사용 우선순위에 따라 적립 건 선택
        val selectedAccumulations = pointUsagePriorityService.selectAccumulationsForUsage(
            memberId = memberId,
            usageAmount = usageAmount,
            accumulations = availableAccumulations
        )
        
        // 포인트 사용 엔티티 생성 및 저장
        val savedUsage = createAndSavePointUsage(memberId, orderNumber, usageAmount)

        // 포인트 사용 처리 및 상세 내역 생성
        val usageDetails = processAccumulationsForUsage(
            accumulations = selectedAccumulations,
            usage = savedUsage,
            totalUsageAmount = usageAmount
        )
        
        // 상세 내역 저장
        pointUsageDetailPersistencePort.saveAll(usageDetails)
        
        // 잔액 업데이트 이벤트 발행
        pointBalanceEventPublisher.publish(
            PointBalanceEvent.Used(
                memberId = memberId,
                amount = usageAmount,
                pointKey = savedUsage.pointKey,
                orderNumber = orderNumber
            )
        )
        
        return savedUsage
    }
    
    /**
     * 포인트 사용 엔티티 생성 및 저장
     */
    private fun createAndSavePointUsage(
        memberId: Long,
        orderNumber: String,
        usageAmount: Money
    ): PointUsage {
        val pointKey = pointKeyGenerator.generate().value
        val usage = PointUsage(
            pointKey = pointKey,
            memberId = memberId,
            orderNumber = OrderNumber.of(orderNumber),
            totalAmount = usageAmount
        )
        return pointUsagePersistencePort.save(usage)
    }
    
    /**
     * 선택된 적립 건들을 순회하며 포인트 사용 처리 및 상세 내역 생성
     * 
     * 적립 건은 상태 변경 후 saveAll을 통해 배치 처리로 일괄 저장합니다.
     * 
     * @param accumulations 사용할 적립 건 목록
     * @param usage 포인트 사용 건
     * @param totalUsageAmount 총 사용 금액
     * @return 생성된 상세 내역 목록
     */
    private fun processAccumulationsForUsage(
        accumulations: List<PointAccumulation>,
        usage: PointUsage,
        totalUsageAmount: Money
    ): List<PointUsageDetail> {
        data class ProcessingState(
            val remainingAmount: Money,
            val usageDetails: List<PointUsageDetail>,
            val updatedAccumulations: List<PointAccumulation>
        )

        val usageId = usage.id ?: throw IllegalStateException("사용 건 ID가 없습니다.")

        val initState = ProcessingState(
            remainingAmount = totalUsageAmount,
            usageDetails = emptyList(),
            updatedAccumulations = emptyList()
        )
        
        val finalState = accumulations.fold(initial = initState) { state, accumulation ->
            val accumulationId = accumulation.id ?: throw IllegalStateException("적립 건 ID가 없습니다.")
            val useAmount = calculateUseAmount(state.remainingAmount, accumulation.availableAmount)

            // 적립 건에서 포인트 사용 처리
            accumulation.use(useAmount)

            // 상세 내역 생성 및 추가
            val detail = createUsageDetail(
                pointUsageId = usageId,
                pointAccumulationId = accumulationId,
                amount = useAmount
            )

            ProcessingState(
                remainingAmount = state.remainingAmount.subtract(useAmount),
                usageDetails = state.usageDetails + detail,
                updatedAccumulations = state.updatedAccumulations + accumulation
            )
        }
        
        // 적립 건 일괄 저장 (배치 처리)
        if (finalState.updatedAccumulations.isNotEmpty()) {
            pointAccumulationPersistencePort.saveAll(finalState.updatedAccumulations)
        }
        
        return finalState.usageDetails
    }
    
    /**
     * 적립 건에서 사용할 금액 계산
     */
    private fun calculateUseAmount(remainingAmount: Money, availableAmount: Money): Money {
        return if (remainingAmount.isLessThan(availableAmount)) {
            remainingAmount
        } else {
            availableAmount
        }
    }
    
    /**
     * 적립 건별 사용 상세 내역 생성
     *
     * 요구사항: "특정 시점에 적립된 포인트는 1원 단위까지 어떤 주문에서 사용되었는지 추적할 수 있어야 한다"
     * - 각 적립 건당 1개의 레코드로 1원 단위 정확도 추적 달성
     * - 성능 최적화: 사용 금액만큼 레코드 생성 → 적립 건당 1개 레코드로 개선
     */
    private fun createUsageDetail(
        pointUsageId: Long,
        pointAccumulationId: Long,
        amount: Money
    ): PointUsageDetail {
        return PointUsageDetail(
            pointUsageId = pointUsageId,
            pointAccumulationId = pointAccumulationId,
            amount = amount
        )
    }
}

