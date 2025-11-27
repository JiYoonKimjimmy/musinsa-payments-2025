package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.input.PointAccumulationUseCase
import com.musinsa.payments.point.application.port.output.PointKeyGenerator
import com.musinsa.payments.point.application.port.output.event.PointBalanceEventPublisher
import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.event.PointBalanceEvent
import com.musinsa.payments.point.domain.exception.InvalidAmountException
import com.musinsa.payments.point.domain.exception.InvalidExpirationDateException
import com.musinsa.payments.point.domain.exception.MaxAccumulationExceededException
import com.musinsa.payments.point.domain.exception.MaxBalanceExceededException
import com.musinsa.payments.point.domain.valueobject.Money
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 포인트 적립 서비스
 * PointAccumulationUseCase 인터페이스를 구현합니다.
 *
 * 성능 최적화:
 * - 코루틴을 사용한 병렬 검증 처리
 * - 독립적인 설정값 조회 및 잔액 조회를 동시 실행
 */
@Transactional
@Service
class PointAccumulationService(
    private val pointAccumulationPersistencePort: PointAccumulationPersistencePort,
    private val pointConfigService: PointConfigService,
    private val pointKeyGenerator: PointKeyGenerator,
    private val pointBalanceEventPublisher: PointBalanceEventPublisher,
    @Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) : PointAccumulationUseCase {
    
    companion object {
        private const val MAX_ACCUMULATION_AMOUNT_PER_TIME = "MAX_ACCUMULATION_AMOUNT_PER_TIME"
        private const val MAX_BALANCE_PER_MEMBER = "MAX_BALANCE_PER_MEMBER"
        private const val DEFAULT_EXPIRATION_DAYS = "DEFAULT_EXPIRATION_DAYS"
        private const val MIN_EXPIRATION_DAYS = "MIN_EXPIRATION_DAYS"
        private const val MAX_EXPIRATION_DAYS = "MAX_EXPIRATION_DAYS"
    }

    override suspend fun accumulate(
        memberId: Long,
        amount: Long,
        expirationDays: Int?,
        isManualGrant: Boolean
    ): PointAccumulation = coroutineScope {
        // 적립 금액 검증
        val moneyAmount = Money.of(amount)
        if (moneyAmount.isLessThanOrEqual(Money.ZERO)) {
            throw InvalidAmountException("적립 금액은 0보다 커야 합니다.")
        }

        // 독립적인 검증 데이터를 병렬로 조회
        val maxAccumulationAmountDeferred = async(ioDispatcher) {
            getConfigLongValue(MAX_ACCUMULATION_AMOUNT_PER_TIME)
        }

        val currentBalanceDeferred = async(ioDispatcher) {
            pointAccumulationPersistencePort.sumAvailableAmountByMemberId(memberId).toLong()
        }

        val maxBalanceDeferred = async(ioDispatcher) {
            getConfigLongValue(MAX_BALANCE_PER_MEMBER)
        }

        // 병렬 조회 결과 대기
        val maxAccumulationAmount = maxAccumulationAmountDeferred.await()
        val currentBalance = currentBalanceDeferred.await()
        val maxBalance = maxBalanceDeferred.await()

        // 최대 적립 금액 검증
        if (amount > maxAccumulationAmount) {
            throw MaxAccumulationExceededException("1회 최대 적립 금액(${maxAccumulationAmount}원)을 초과했습니다. 요청 금액: ${amount}원")
        }

        // 최대 보유 금액 검증
        if (currentBalance + amount > maxBalance) {
            throw MaxBalanceExceededException()
        }

        // 만료일 계산 및 검증
        val expirationDate = calculateExpirationDate(expirationDays)
        validateExpirationDate(expirationDate)

        // 포인트 키 생성
        val pointKey = pointKeyGenerator.generate().value

        // 포인트 적립 엔티티 생성
        val accumulation = PointAccumulation(
            pointKey = pointKey,
            memberId = memberId,
            amount = moneyAmount,
            expirationDate = expirationDate,
            isManualGrant = isManualGrant
        )

        // 저장
        val saved = pointAccumulationPersistencePort.save(accumulation)

        // 잔액 업데이트 이벤트 발행
        pointBalanceEventPublisher.publish(
            PointBalanceEvent.Accumulated(
                memberId = memberId,
                amount = moneyAmount,
                pointKey = pointKey
            )
        )

        saved
    }

    override suspend fun cancelAccumulation(
        pointKey: String,
        reason: String?
    ): PointAccumulation {
        // 적립 건 조회
        val accumulation = pointAccumulationPersistencePort.findByPointKey(pointKey)
            .orElseThrow { IllegalArgumentException("포인트 적립 건을 찾을 수 없습니다: $pointKey") }

        // 취소할 금액 (사용 가능 잔액)
        val cancelAmount = accumulation.availableAmount

        // 적립 취소 처리
        accumulation.cancel()

        // 저장
        val saved = pointAccumulationPersistencePort.save(accumulation)

        // 잔액 업데이트 이벤트 발행 (취소 금액이 있는 경우에만)
        if (cancelAmount.isGreaterThan(Money.ZERO)) {
            pointBalanceEventPublisher.publish(
                PointBalanceEvent.AccumulationCancelled(
                    memberId = accumulation.memberId,
                    amount = cancelAmount,
                    pointKey = pointKey
                )
            )
        }

        return saved
    }
    
    /**
     * 만료일 계산
     */
    private fun calculateExpirationDate(expirationDays: Int?): LocalDate {
        val days = expirationDays ?: getConfigIntValue(DEFAULT_EXPIRATION_DAYS)
        return LocalDate.now().plusDays(days.toLong())
    }
    
    /**
     * 만료일 검증
     */
    private fun validateExpirationDate(expirationDate: LocalDate) {
        val minDays = getConfigIntValue(MIN_EXPIRATION_DAYS)
        val maxDays = getConfigIntValue(MAX_EXPIRATION_DAYS)
        val today = LocalDate.now()
        
        val actualDays = java.time.temporal.ChronoUnit.DAYS.between(today, expirationDate).toInt()
        
        if (actualDays < minDays) {
            throw InvalidExpirationDateException("만료일은 최소 ${minDays}일 이후여야 합니다. (현재: ${actualDays}일)")
        }
        
        if (actualDays > maxDays) {
            throw InvalidExpirationDateException("만료일은 최대 ${maxDays}일 이하여야 합니다. (현재: ${actualDays}일)")
        }
    }
    
    /**
     * 설정 값을 Long 타입으로 조회
     */
    private fun getConfigLongValue(configKey: String): Long {
        return pointConfigService.getLongValue(configKey)
    }
    
    /**
     * 설정 값을 Int 타입으로 조회
     */
    private fun getConfigIntValue(configKey: String): Int {
        return pointConfigService.getIntValue(configKey)
    }
}

