package com.musinsa.payments.point.application.event

import com.musinsa.payments.point.application.service.PointBalanceReconciliationService
import com.musinsa.payments.point.domain.event.BalanceReconciliationRequestEvent
import com.musinsa.payments.point.infrastructure.config.AsyncConfig
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * 잔액 보정 요청 이벤트 핸들러
 * 캐시 업데이트 실패 시 발행된 보정 요청 이벤트를 처리합니다.
 */
@Component
class BalanceReconciliationEventHandler(
    private val reconciliationService: PointBalanceReconciliationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 잔액 보정 요청 이벤트 처리
     * SUM 쿼리로 실제 잔액을 계산하여 캐시를 보정합니다.
     */
    @Async(AsyncConfig.POINT_EVENT_EXECUTOR)
    @EventListener
    fun handleReconciliationRequest(event: BalanceReconciliationRequestEvent) {
        logger.info(
            "잔액 보정 요청 수신: memberId=${event.memberId}, " +
            "reason=${event.reason}, originalEventType=${event.originalEventType}"
        )
        
        try {
            val result = reconciliationService.reconcileMemberBalance(event.memberId)
            logger.info(
                "잔액 보정 완료: memberId=${event.memberId}, status=${result.status}, " +
                "actual=${result.actualBalance}, cached=${result.cachedBalance}, " +
                "difference=${result.difference}"
            )
        } catch (e: Exception) {
            logger.error("잔액 보정 실패: memberId=${event.memberId}, error=${e.message}", e)
            // 보정도 실패하면 알림 발송 등 추가 조치 필요
        }
    }
}

