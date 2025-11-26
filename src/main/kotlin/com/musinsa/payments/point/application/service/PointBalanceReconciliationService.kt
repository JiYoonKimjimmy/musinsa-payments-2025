package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.persistence.MemberPointBalancePersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.domain.entity.MemberPointBalance
import com.musinsa.payments.point.domain.valueobject.Money
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 잔액 정합성 보정 서비스
 * 회원별 잔액 테이블과 실제 적립 건의 잔액을 비교하여 불일치 시 보정합니다.
 */
@Service
class PointBalanceReconciliationService(
    private val memberPointBalancePersistencePort: MemberPointBalancePersistencePort,
    private val pointAccumulationPersistencePort: PointAccumulationPersistencePort
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 잔액 정합성 검증 결과
     */
    data class ReconciliationResult(
        val memberId: Long,
        val status: ReconciliationStatus,
        val actualBalance: Money,
        val cachedBalance: Money,
        val difference: Money
    )

    /**
     * 보정 상태
     */
    enum class ReconciliationStatus {
        MATCHED,    // 일치
        CORRECTED,  // 불일치 -> 보정됨
        CREATED,    // 신규 생성됨
        SKIPPED     // 건너뜀 (잔액 없음)
    }

    /**
     * 모든 회원의 잔액 정합성 검증 및 보정
     * @return 보정 결과 목록
     */
    @Transactional
    fun reconcileAllBalances(): List<ReconciliationResult> {
        val allBalances = memberPointBalancePersistencePort.findAll()

        return allBalances
            .map { reconcileMemberBalance(it.memberId) }
    }

    /**
     * 특정 회원의 잔액 정합성 검증 및 보정
     * @param memberId 회원 ID
     * @return 보정 결과
     */
    @Transactional
    fun reconcileMemberBalance(memberId: Long): ReconciliationResult {
        // 실제 잔액 계산 (SUM 쿼리)
        val actualBalance = pointAccumulationPersistencePort.sumAvailableAmountByMemberId(memberId)

        // 캐시된 잔액 조회
        val cachedBalanceOpt = memberPointBalancePersistencePort.findByMemberIdWithLock(memberId)

        return if (cachedBalanceOpt.isPresent) {
            val cachedBalance = cachedBalanceOpt.get()
            val cachedAmount = cachedBalance.availableBalance

            if (actualBalance != cachedAmount) {
                // 불일치 발견 - 보정
                logger.warn("잔액 불일치 발견: memberId=$memberId, 실제=$actualBalance, 캐시=$cachedAmount")

                cachedBalance.reconcile(actualBalance)

                memberPointBalancePersistencePort.save(cachedBalance)

                ReconciliationResult(
                    memberId = memberId,
                    status = ReconciliationStatus.CORRECTED,
                    actualBalance = actualBalance,
                    cachedBalance = cachedAmount,
                    difference = actualBalance.subtract(cachedAmount)
                )
            } else {
                // 일치
                ReconciliationResult(
                    memberId = memberId,
                    status = ReconciliationStatus.MATCHED,
                    actualBalance = actualBalance,
                    cachedBalance = cachedAmount,
                    difference = Money.ZERO
                )
            }
        } else {
            // 캐시된 잔액이 없음 - 신규 생성
            if (actualBalance.isGreaterThan(Money.ZERO)) {
                val newBalance = MemberPointBalance(memberId)

                newBalance.reconcile(actualBalance)

                memberPointBalancePersistencePort.save(newBalance)

                logger.info("잔액 캐시 신규 생성: memberId={}, 잔액={}", memberId, actualBalance)

                ReconciliationResult(
                    memberId = memberId,
                    status = ReconciliationStatus.CREATED,
                    actualBalance = actualBalance,
                    cachedBalance = Money.ZERO,
                    difference = actualBalance
                )
            } else {
                ReconciliationResult(
                    memberId = memberId,
                    status = ReconciliationStatus.SKIPPED,
                    actualBalance = actualBalance,
                    cachedBalance = Money.ZERO,
                    difference = Money.ZERO
                )
            }
        }
    }
}

