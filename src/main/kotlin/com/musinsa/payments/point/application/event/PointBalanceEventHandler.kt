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
 * - 각 이벤트가 action() 메서드로 자신의 행동을 캡슐화하여 Open-Closed Principle 준수
 */
@Component
class PointBalanceEventHandler(
    private val cacheUpdateService: PointBalanceCacheUpdateService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async(AsyncConfig.POINT_EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePointBalanceEvent(event: PointBalanceEvent) {
        logger.debug("포인트 ${event.eventTypeName} 이벤트 수신: memberId=${event.memberId}")
        cacheUpdateService.updateBalanceWithRetry(event = event, action = event::action)
    }
}
