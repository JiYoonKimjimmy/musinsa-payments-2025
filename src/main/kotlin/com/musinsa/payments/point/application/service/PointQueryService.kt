package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.input.PointBalanceResult
import com.musinsa.payments.point.application.port.input.PointQueryUseCase
import com.musinsa.payments.point.application.port.output.persistence.MemberPointBalancePersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.domain.entity.PointUsage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
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
 *
 * 성능 최적화:
 * - 코루틴을 사용한 비동기 병렬 처리
 * - 독립적인 DB 쿼리를 동시 실행
 * - CPU 집약적 계산 병렬화
 */
@Transactional(readOnly = true)
@Service
class PointQueryService(
    private val pointAccumulationPersistencePort: PointAccumulationPersistencePort,
    private val pointUsagePersistencePort: PointUsagePersistencePort,
    private val memberPointBalancePersistencePort: MemberPointBalancePersistencePort,
    @Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
    @Qualifier("cpuDispatcher") private val cpuDispatcher: CoroutineDispatcher
) : PointQueryUseCase {
    
    override suspend fun getBalance(memberId: Long): PointBalanceResult = coroutineScope {
        // 독립적인 DB 쿼리를 병렬로 실행
        val accumulationsDeferred = async(ioDispatcher) {
            pointAccumulationPersistencePort.findByMemberIdAndStatus(memberId, PointAccumulationStatus.ACCUMULATED)
        }

        val cachedBalanceDeferred = async(ioDispatcher) {
            memberPointBalancePersistencePort.findByMemberId(memberId)
        }

        // 쿼리 결과 대기
        val allAccumulations = accumulationsDeferred.await()
        val cachedBalance = cachedBalanceDeferred.await()

        // CPU 집약적 계산을 병렬로 실행
        val totalBalanceDeferred = async(cpuDispatcher) {
            allAccumulations.sumOf { it.amount.toLong() }
        }

        val expiredBalanceDeferred = async(cpuDispatcher) {
            allAccumulations
                .filter { it.isExpired() }
                .sumOf { it.availableAmount.toLong() }
        }

        val availableBalanceDeferred = async(cpuDispatcher) {
            if (cachedBalance.isPresent) {
                cachedBalance.get().availableBalance.toLong()
            } else {
                // Fallback: 적립 건에서 직접 계산
                allAccumulations
                    .filter { !it.isExpired() }
                    .sumOf { it.availableAmount.toLong() }
            }
        }

        // 모든 계산 결과 대기 및 반환
        PointBalanceResult(
            memberId = memberId,
            totalBalance = totalBalanceDeferred.await(),
            availableBalance = availableBalanceDeferred.await(),
            expiredBalance = expiredBalanceDeferred.await(),
            accumulations = allAccumulations
        )
    }
    
    override suspend fun getUsageHistory(
        memberId: Long,
        orderNumber: String?,
        pageable: Pageable
    ): Page<PointUsage> = withContext(ioDispatcher) {
        pointUsagePersistencePort.findUsageHistoryByMemberId(
            memberId = memberId,
            orderNumber = orderNumber,
            pageable = pageable
        )
    }
}

