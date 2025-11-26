package com.musinsa.payments.point.infrastructure.event

import com.musinsa.payments.point.application.port.output.event.PointBalanceEventPublisher
import com.musinsa.payments.point.domain.event.PointBalanceEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Spring ApplicationEventPublisher 기반 이벤트 발행 어댑터
 * Infrastructure 레이어에서 PointBalanceEventPublisher 인터페이스를 구현합니다.
 */
@Component
class SpringPointBalanceEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : PointBalanceEventPublisher {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override fun publish(event: PointBalanceEvent) {
        logger.debug("포인트 잔액 이벤트 발행: {}", event)
        applicationEventPublisher.publishEvent(event)
    }
}

