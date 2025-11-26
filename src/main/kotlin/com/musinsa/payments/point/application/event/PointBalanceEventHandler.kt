package com.musinsa.payments.point.application.event

import com.musinsa.payments.point.application.port.output.persistence.MemberPointBalancePersistencePort
import com.musinsa.payments.point.domain.entity.MemberPointBalance
import com.musinsa.payments.point.domain.event.PointBalanceEvent
import com.musinsa.payments.point.infrastructure.config.AsyncConfig
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 포인트 잔액 이벤트 핸들러
 * 포인트 거래 이벤트를 수신하여 회원별 잔액을 업데이트합니다.
 * 
 * - @TransactionalEventListener(phase = AFTER_COMMIT): 원본 트랜잭션 커밋 후 실행
 * - @Async: 비동기로 실행하여 메인 트랜잭션 응답 시간에 영향 없음
 * - @Transactional(propagation = REQUIRES_NEW): 새로운 트랜잭션에서 실행
 */
@Component
class PointBalanceEventHandler(
    private val memberPointBalancePersistencePort: MemberPointBalancePersistencePort
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async(AsyncConfig.POINT_EVENT_EXECUTOR)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleAccumulated(event: PointBalanceEvent.Accumulated) =
        processEvent(event) { it.addBalance(event.amount) }

    @Async(AsyncConfig.POINT_EVENT_EXECUTOR)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleAccumulationCancelled(event: PointBalanceEvent.AccumulationCancelled) =
        processEvent(event) { it.cancelAccumulation(event.amount) }

    @Async(AsyncConfig.POINT_EVENT_EXECUTOR)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleUsed(event: PointBalanceEvent.Used) =
        processEvent(event) { it.subtractBalance(event.amount) }

    @Async(AsyncConfig.POINT_EVENT_EXECUTOR)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleUsageCancelled(event: PointBalanceEvent.UsageCancelled) =
        processEvent(event) { it.restoreBalance(event.amount) }

    @Async(AsyncConfig.POINT_EVENT_EXECUTOR)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleExpired(event: PointBalanceEvent.Expired) =
        processEvent(event) { it.expireBalance(event.amount) }

    /**
     * 이벤트 처리 공통 로직
     */
    private fun processEvent(event: PointBalanceEvent, action: (MemberPointBalance) -> Unit) {
        logger.info("포인트 ${event.eventTypeName} 이벤트 처리 시작: memberId=${event.memberId}, amount=${event.amount}, pointKey=${event.pointKey}${event.additionalLogInfo}")
        
        try {
            val balance = memberPointBalancePersistencePort
                .findByMemberIdWithLock(event.memberId)
                .orElseGet { MemberPointBalance(event.memberId) }
            
            action(balance)
            memberPointBalancePersistencePort.save(balance)
            
            logger.info("포인트 ${event.eventTypeName} 이벤트 처리 완료: memberId=${event.memberId}")
        } catch (e: Exception) {
            logger.error("포인트 ${event.eventTypeName} 이벤트 처리 실패: memberId=${event.memberId}, error=${e.message}", e)
            throw e
        }
    }
}

