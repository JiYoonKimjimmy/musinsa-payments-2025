package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.persistence.MemberPointBalancePersistencePort
import com.musinsa.payments.point.domain.entity.MemberPointBalance
import com.musinsa.payments.point.domain.event.BalanceReconciliationRequestEvent
import com.musinsa.payments.point.domain.event.PointBalanceEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 잔액 캐시 업데이트 서비스
 * 재시도 로직이 포함된 캐시 업데이트를 수행합니다.
 * 
 * - 최대 3회 재시도 (지수 백오프: 1초 → 2초 → 4초)
 * - 최종 실패 시 잔액 보정 요청 이벤트 발행
 */
@Service
class PointBalanceCacheUpdateService(
    private val memberPointBalancePersistencePort: MemberPointBalancePersistencePort,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 재시도가 포함된 잔액 캐시 업데이트
     * 
     * @param event 포인트 잔액 이벤트
     * @param action 잔액에 대해 수행할 액션
     */
    @Retryable(
        retryFor = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updateBalanceWithRetry(event: PointBalanceEvent, action: (MemberPointBalance) -> Unit) {
        logger.info(
            "포인트 ${event.eventTypeName} 이벤트 처리 시작: " +
            "memberId=${event.memberId}, amount=${event.amount}, pointKey=${event.pointKey}${event.additionalLogInfo}"
        )
        
        val balance = memberPointBalancePersistencePort.findByMemberIdWithLock(event.memberId)
            .orElseGet { MemberPointBalance(event.memberId) }
        
        action(balance)

        memberPointBalancePersistencePort.save(balance)
        
        logger.info("포인트 ${event.eventTypeName} 이벤트 처리 완료: memberId=${event.memberId}")
    }
    
    /**
     * 재시도 모두 실패 시 실행되는 복구 메서드
     * 잔액 보정 요청 이벤트를 발행합니다.
     * 
     * @param e 발생한 예외
     * @param event 원본 포인트 잔액 이벤트
     * @param action 수행하려던 액션 (사용되지 않음)
     */
    @Recover
    fun recoverFromFailure(
        e: Exception,
        event: PointBalanceEvent,
        @Suppress("UNUSED_PARAMETER") action: (MemberPointBalance) -> Unit
    ) {
        logger.error(
            "포인트 ${event.eventTypeName} 이벤트 처리 최종 실패. 보정 요청 발행: " +
            "memberId=${event.memberId}, pointKey=${event.pointKey}, error=${e.message}",
            e
        )
        
        // 잔액 보정 요청 이벤트 발행
        eventPublisher.publishEvent(
            BalanceReconciliationRequestEvent(
                memberId = event.memberId,
                reason = "캐시 업데이트 실패 (${event.eventTypeName}): ${e.message}",
                originalEventType = event.eventTypeName
            )
        )
    }
}

