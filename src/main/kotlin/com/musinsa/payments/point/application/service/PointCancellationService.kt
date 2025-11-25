package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.input.PointCancellationUseCase
import com.musinsa.payments.point.application.port.output.PointKeyGenerator
import com.musinsa.payments.point.application.port.output.config.PointConfigPort
import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsageDetailPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.valueobject.Money
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 포인트 사용 취소 서비스
 * PointCancellationUseCase 인터페이스를 구현합니다.
 */
@Transactional
@Service
class PointCancellationService(
    private val pointUsagePersistencePort: PointUsagePersistencePort,
    private val pointUsageDetailPersistencePort: PointUsageDetailPersistencePort,
    private val pointAccumulationPersistencePort: PointAccumulationPersistencePort,
    private val pointKeyGenerator: PointKeyGenerator,
    private val pointConfigPort: PointConfigPort
) : PointCancellationUseCase {
    
    companion object {
        private const val DEFAULT_EXPIRATION_DAYS = "DEFAULT_EXPIRATION_DAYS"
    }

    override fun cancelUsage(
        pointKey: String,
        amount: Long?,
        reason: String?
    ): com.musinsa.payments.point.domain.entity.PointUsage {
        // 사용 건 조회
        val usage = pointUsagePersistencePort
            .findByPointKey(pointKey)
            .orElseThrow { IllegalArgumentException("포인트 사용 건을 찾을 수 없습니다: $pointKey") }
        
        // 취소 금액 결정 (null이면 전체 취소)
        val cancelAmount = if (amount != null) {
            Money.of(amount)
        } else {
            usage.getRemainingAmount()
        }
        
        // 취소 가능 여부 확인
        if (!usage.canCancel(cancelAmount)) {
            throw com.musinsa.payments.point.domain.exception.CannotCancelUsageException()
        }
        
        // 사용 상세 내역 조회
        val usageDetails = pointUsageDetailPersistencePort.findByUsagePointKey(pointKey)
        
        // 상세 내역별로 취소 처리
        var remainingCancelAmount = cancelAmount
        val restoredAccumulations = mutableMapOf<Long, Money>()
        
        for (detail in usageDetails) {
            if (remainingCancelAmount.isLessThanOrEqual(Money.ZERO)) {
                break
            }
            
            val detailRemaining = detail.getRemainingAmount()
            if (detailRemaining.isLessThanOrEqual(Money.ZERO)) {
                continue
            }
            
            val detailCancelAmount = if (remainingCancelAmount.isLessThan(detailRemaining)) {
                remainingCancelAmount
            } else {
                detailRemaining
            }
            
            // 상세 내역 취소 처리
            detail.cancel(detailCancelAmount)
            
            // 적립 건 복원 금액 누적
            val accumulationId = detail.pointAccumulationId
            val currentRestoreAmount = restoredAccumulations.getOrDefault(accumulationId, Money.ZERO)
            restoredAccumulations[accumulationId] = currentRestoreAmount.add(detailCancelAmount)
            
            remainingCancelAmount = remainingCancelAmount.subtract(detailCancelAmount)
        }
        
        // 적립 건 복원 처리 (만료 포인트 확인 및 신규 적립 처리)
        for ((accumulationId, restoreAmount) in restoredAccumulations) {
            restoreAccumulation(accumulationId, restoreAmount)
        }
        
        // 사용 건 취소 처리
        usage.cancel(cancelAmount)
        
        // 저장
        val savedUsage = pointUsagePersistencePort.save(usage)
        pointUsageDetailPersistencePort.saveAll(usageDetails)
        
        return savedUsage
    }
    
    /**
     * 적립 건 복원 처리
     * 만료된 포인트인 경우 신규 적립으로 처리하고, 그렇지 않으면 기존 적립 건 복원
     */
    private fun restoreAccumulation(accumulationId: Long, restoreAmount: Money) {
        // 적립 건 조회
        val accumulation = pointAccumulationPersistencePort
            .findById(accumulationId)
            .orElseThrow { IllegalArgumentException("포인트 적립 건을 찾을 수 없습니다: $accumulationId") }
        
        // 만료 여부 확인
        if (accumulation.isExpired()) {
            // 만료된 포인트는 신규 적립으로 처리
            val newAccumulation = createNewAccumulationForExpiredPoint(
                memberId = accumulation.memberId,
                amount = restoreAmount,
                originalExpirationDate = accumulation.expirationDate
            )
            pointAccumulationPersistencePort.save(newAccumulation)
        } else {
            // 만료되지 않은 포인트는 기존 적립 건 복원
            accumulation.restore(restoreAmount)
            pointAccumulationPersistencePort.save(accumulation)
        }
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
        val defaultExpirationDays = getConfigIntValue(DEFAULT_EXPIRATION_DAYS)
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
    
    /**
     * 설정 값을 Int 타입으로 조회
     */
    private fun getConfigIntValue(configKey: String): Int {
        return pointConfigPort.findByConfigKey(configKey)
            .orElseThrow { IllegalArgumentException("설정을 찾을 수 없습니다: $configKey") }
            .getIntValue()
    }
}

