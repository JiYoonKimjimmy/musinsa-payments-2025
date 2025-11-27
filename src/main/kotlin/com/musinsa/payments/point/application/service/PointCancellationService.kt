package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.input.PointCancellationUseCase
import com.musinsa.payments.point.application.port.output.PointKeyGenerator
import com.musinsa.payments.point.application.port.output.event.PointBalanceEventPublisher
import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsageDetailPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.entity.PointUsageDetail
import com.musinsa.payments.point.domain.event.PointBalanceEvent
import com.musinsa.payments.point.domain.exception.CannotCancelUsageException
import com.musinsa.payments.point.domain.valueobject.Money
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 포인트 사용 취소 서비스
 * PointCancellationUseCase 인터페이스를 구현합니다.
 */
@Transactional(isolation = Isolation.READ_COMMITTED)
@Service
class PointCancellationService(
    private val pointUsagePersistencePort: PointUsagePersistencePort,
    private val pointUsageDetailPersistencePort: PointUsageDetailPersistencePort,
    private val pointAccumulationPersistencePort: PointAccumulationPersistencePort,
    private val pointKeyGenerator: PointKeyGenerator,
    private val pointConfigService: PointConfigService,
    private val pointBalanceEventPublisher: PointBalanceEventPublisher
) : PointCancellationUseCase {
    
    companion object {
        private const val DEFAULT_EXPIRATION_DAYS = "DEFAULT_EXPIRATION_DAYS"
    }

    /**
     * 포인트 사용 취소 처리
     * 
     * @param pointKey 포인트 사용 키
     * @param amount 취소할 금액 (null이면 전체 취소)
     * @param reason 취소 사유 (현재 미사용)
     * @return 취소 처리된 포인트 사용 건
     */
    override fun cancelUsage(
        pointKey: String,
        amount: Long?,
        reason: String?
    ): PointUsage {
        // 1. 사용 건 조회 및 검증
        val usage = findUsage(pointKey)
        val cancelAmount = determineCancelAmount(usage, amount)
        validateCancellation(usage, cancelAmount)
        
        // 2. 상세 내역 조회 및 취소 처리, 적립 건별 복원 금액 계산
        val usageDetails = pointUsageDetailPersistencePort.findByUsagePointKey(pointKey)
        val restoredAccumulations = calculateRestoreAmounts(usageDetails, cancelAmount)
        
        // 3. 적립 건 복원 처리 (만료 포인트는 신규 적립으로 처리)
        restoreAccumulations(restoredAccumulations)
        usage.cancel(cancelAmount)
        
        // 4. 저장 및 이벤트 발행
        val savedUsage = saveUsageAndDetails(usage, usageDetails)
        publishCancellationEvent(usage.memberId, cancelAmount, pointKey)
        
        return savedUsage
    }
    
    /**
     * 사용 건 조회
     */
    private fun findUsage(pointKey: String): PointUsage {
        return pointUsagePersistencePort.findByPointKey(pointKey)
            .orElseThrow { IllegalArgumentException("포인트 사용 건을 찾을 수 없습니다: $pointKey") }
    }
    
    /**
     * 취소 금액 결정 (null이면 전체 취소)
     */
    private fun determineCancelAmount(usage: PointUsage, amount: Long?): Money {
        return amount?.let { Money.of(it) } ?: usage.getRemainingAmount()
    }
    
    /**
     * 취소 가능 여부 확인
     */
    private fun validateCancellation(usage: PointUsage, cancelAmount: Money) {
        if (!usage.canCancel(cancelAmount)) {
            throw CannotCancelUsageException()
        }
    }
    
    /**
     * 사용 건 및 상세 내역 저장
     */
    private fun saveUsageAndDetails(usage: PointUsage, usageDetails: List<PointUsageDetail>): PointUsage {
        val savedUsage = pointUsagePersistencePort.save(usage)
        pointUsageDetailPersistencePort.saveAll(usageDetails)
        return savedUsage
    }
    
    /**
     * 취소 이벤트 발행
     */
    private fun publishCancellationEvent(memberId: Long, cancelAmount: Money, pointKey: String) {
        pointBalanceEventPublisher.publish(
            PointBalanceEvent.UsageCancelled(
                memberId = memberId,
                amount = cancelAmount,
                pointKey = pointKey
            )
        )
    }
    
    /**
     * 상세 내역별로 취소 처리 및 적립 건별 복원 금액 계산
     * 
     * @param usageDetails 사용 상세 내역 목록
     * @param cancelAmount 취소할 금액
     * @return 적립 건별 복원 금액 맵
     */
    private fun calculateRestoreAmounts(
        usageDetails: List<PointUsageDetail>,
        cancelAmount: Money
    ): Map<Long, Money> {
        return usageDetails
            // 취소 가능한 상세 내역만 필터링
            .filter { it.getRemainingAmount().isGreaterThan(Money.ZERO) }
            // fold를 사용하여 취소 금액과 적립 건별 복원 금액을 누적 계산
            .fold(cancelAmount to emptyMap<Long, Money>()) { (remaining, acc), detail ->
                // 남은 취소 금액이 없으면 조기 종료
                if (remaining.isLessThanOrEqual(Money.ZERO)) {
                    return@fold remaining to acc
                }
                
                // 상세 내역별 취소 금액 계산 (남은 금액과 상세 내역 남은 금액 중 작은 값)
                val detailRemaining = detail.getRemainingAmount()
                val detailCancelAmount = if (remaining.isLessThan(detailRemaining)) {
                    remaining
                } else {
                    detailRemaining
                }
                
                // 상세 내역 취소 처리
                detail.cancel(detailCancelAmount)
                
                // 적립 건별 복원 금액 누적
                val accumulationId = detail.pointAccumulationId
                val currentRestoreAmount = acc.getOrDefault(accumulationId, Money.ZERO)
                val newRestoreAmount = currentRestoreAmount.add(detailCancelAmount)
                val newAcc = acc + (accumulationId to newRestoreAmount)
                
                // 다음 반복을 위한 값 반환 (남은 금액 감소, 맵 업데이트)
                remaining.subtract(detailCancelAmount) to newAcc
            }
            .second // Pair에서 맵만 추출
    }
    
    /**
     * 적립 건 복원 처리 (배치 조회 및 복원)
     * 
     * @param restoredAccumulations 적립 건별 복원 금액 맵
     */
    private fun restoreAccumulations(restoredAccumulations: Map<Long, Money>) {
        if (restoredAccumulations.isEmpty()) {
            return
        }
        
        // N+1 문제 방지를 위해 배치 조회 처리
        val accumulationIds = restoredAccumulations.keys.toList()
        val accumulationsMap = pointAccumulationPersistencePort.findByIdsWithLock(accumulationIds)
        
        restoredAccumulations.forEach { (accumulationId, restoreAmount) ->
            val accumulation = accumulationsMap.getValue(accumulationId)
            restoreAccumulation(accumulation, restoreAmount)
        }
    }
    
    /**
     * 적립 건 복원 처리
     * 만료된 포인트인 경우 신규 적립으로 처리하고, 그렇지 않으면 기존 적립 건 복원
     * 
     * @param accumulation 조회된 적립 건 (이미 락이 적용된 상태)
     * @param restoreAmount 복원할 금액
     */
    private fun restoreAccumulation(accumulation: PointAccumulation, restoreAmount: Money) {
        val restored = when {
            accumulation.isExpired() -> {
                // 만료된 포인트는 신규 적립으로 처리
                createNewAccumulationForExpiredPoint(
                    memberId = accumulation.memberId,
                    amount = restoreAmount,
                    originalExpirationDate = accumulation.expirationDate
                )
            }
            else -> {
                // 만료되지 않은 포인트는 기존 적립 건 복원
                accumulation.restore(restoreAmount)
            }
        }
        pointAccumulationPersistencePort.save(restored)
    }
    
    /**
     * 만료 포인트 확인 및 신규 적립 처리
     */
    private fun createNewAccumulationForExpiredPoint(
        memberId: Long,
        amount: Money,
        originalExpirationDate: LocalDate
    ): PointAccumulation {
        // 만료일이 지났는지 확인
        val today = LocalDate.now()
        require(today.isAfter(originalExpirationDate)) { "만료되지 않은 포인트입니다." }
        
        // 만료된 포인트는 신규 적립으로 처리
        val defaultExpirationDays = pointConfigService.getIntValue(DEFAULT_EXPIRATION_DAYS)
        val newExpirationDate = today.plusDays(defaultExpirationDays.toLong())
        
        val pointKey = pointKeyGenerator.generate().value
        
        return PointAccumulation(
            pointKey = pointKey,
            memberId = memberId,
            amount = amount,
            expirationDate = newExpirationDate,
            isManualGrant = false
        )
    }
}

