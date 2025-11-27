package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.persistence.MemberPointBalancePersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.domain.entity.MemberPointBalance
import com.musinsa.payments.point.domain.valueobject.Money
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 잔액 정합성 보정 서비스
 * 회원별 잔액 테이블과 실제 적립 건의 잔액을 비교하여 불일치 시 보정합니다.
 *
 * 성능 최적화:
 * - 코루틴을 사용한 병렬 처리
 * - 청크 단위로 제어된 동시성 처리
 * - 각 회원은 독립적인 트랜잭션에서 처리
 */
@Service
class PointBalanceReconciliationService(
    private val memberPointBalancePersistencePort: MemberPointBalancePersistencePort,
    private val pointAccumulationPersistencePort: PointAccumulationPersistencePort,
    @Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        /**
         * 동시 처리할 청크 크기
         * 너무 크면 DB 커넥션 부족, 너무 작으면 효율 저하
         */
        private const val CHUNK_SIZE = 50
    }

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
     * 모든 회원의 잔액 정합성 검증 및 보정 (병렬 처리)
     *
     * 청크 단위로 회원을 병렬 처리하여 성능을 개선합니다.
     * 각 회원은 독립적인 트랜잭션에서 처리되므로 일부 실패해도 다른 회원에 영향 없습니다.
     *
     * @return 보정 결과 목록
     */
    suspend fun reconcileAllBalances(): List<ReconciliationResult> = coroutineScope {
        val allBalances = memberPointBalancePersistencePort.findAll()

        logger.info("전체 회원 잔액 정합성 검증 시작: 총 {} 명", allBalances.size)

        // 청크 단위로 분할하여 병렬 처리
        allBalances
            .chunked(CHUNK_SIZE)
            .flatMap { chunk ->
                logger.debug("청크 처리 시작: {} 명", chunk.size)

                // 각 청크 내에서 병렬로 처리
                chunk.map { balance ->
                    async(ioDispatcher) {
                        try {
                            reconcileMemberBalance(balance.memberId)
                        } catch (e: Exception) {
                            logger.error("회원 잔액 보정 실패: memberId=${balance.memberId}, 오류=${e.message}", e)
                            // 실패 시 SKIPPED 상태로 반환
                            ReconciliationResult(
                                memberId = balance.memberId,
                                status = ReconciliationStatus.SKIPPED,
                                actualBalance = Money.ZERO,
                                cachedBalance = Money.ZERO,
                                difference = Money.ZERO
                            )
                        }
                    }
                }.awaitAll()
            }
    }

    /**
     * 특정 회원의 잔액 정합성 검증 및 보정
     *
     * 각 회원은 독립적인 트랜잭션에서 처리됩니다.
     * REQUIRES_NEW를 사용하여 병렬 처리 시에도 각각 별도의 트랜잭션으로 동작하도록 합니다.
     *
     * @param memberId 회원 ID
     * @return 보정 결과
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

