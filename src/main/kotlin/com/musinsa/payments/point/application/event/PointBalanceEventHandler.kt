package com.musinsa.payments.point.application.event

import com.musinsa.payments.point.application.service.PointBalanceCacheUpdateService
import com.musinsa.payments.point.domain.event.PointBalanceEvent
import com.musinsa.payments.point.infrastructure.config.AsyncConfig
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 포인트 잔액 이벤트 핸들러
 * 포인트 거래 이벤트를 수신하여 회원별 잔액을 업데이트합니다.
 * 
 * - @TransactionalEventListener(phase = AFTER_COMMIT): 원본 트랜잭션 커밋 후 실행
 * - @Async: 비동기로 실행하여 메인 트랜잭션 응답 시간에 영향 없음
 * - 재시도 및 복구 로직은 PointBalanceCacheUpdateService에서 처리
 */
@Component
class PointBalanceEventHandler(
    private val cacheUpdateService: PointBalanceCacheUpdateService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async(AsyncConfig.POINT_EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleAccumulated(event: PointBalanceEvent.Accumulated) {
        logger.debug("적립 이벤트 수신: memberId=${event.memberId}, pointKey=${event.pointKey}")
        cacheUpdateService.updateBalanceWithRetry(event) { it.addBalance(event.amount) }
    }

    @Async(AsyncConfig.POINT_EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleAccumulationCancelled(event: PointBalanceEvent.AccumulationCancelled) {
        logger.debug("적립 취소 이벤트 수신: memberId=${event.memberId}, pointKey=${event.pointKey}")
        cacheUpdateService.updateBalanceWithRetry(event) { it.cancelAccumulation(event.amount) }
    }

    @Async(AsyncConfig.POINT_EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleUsed(event: PointBalanceEvent.Used) {
        logger.debug("사용 이벤트 수신: memberId=${event.memberId}, pointKey=${event.pointKey}")
        cacheUpdateService.updateBalanceWithRetry(event) { it.subtractBalance(event.amount) }
    }

    @Async(AsyncConfig.POINT_EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleUsageCancelled(event: PointBalanceEvent.UsageCancelled) {
        logger.debug("사용 취소 이벤트 수신: memberId=${event.memberId}, pointKey=${event.pointKey}")
        cacheUpdateService.updateBalanceWithRetry(event) { it.restoreBalance(event.amount) }
    }

    @Async(AsyncConfig.POINT_EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleExpired(event: PointBalanceEvent.Expired) {
        logger.debug("만료 이벤트 수신: memberId=${event.memberId}, pointKey=${event.pointKey}")
        cacheUpdateService.updateBalanceWithRetry(event) { it.expireBalance(event.amount) }
    }
}
