package com.musinsa.payments.point.application.port.output.event.fixtures

import com.musinsa.payments.point.application.port.output.event.PointBalanceEventPublisher
import com.musinsa.payments.point.domain.event.PointBalanceEvent

/**
 * 포인트 잔액 이벤트 발행기의 Fake 구현체
 * 테스트에서 발행된 이벤트를 기록하고 검증할 수 있습니다.
 */
class FakePointBalanceEventPublisher : PointBalanceEventPublisher {
    
    private val publishedEvents = mutableListOf<PointBalanceEvent>()
    
    override fun publish(event: PointBalanceEvent) {
        publishedEvents.add(event)
    }
    
    /**
     * 발행된 모든 이벤트 조회
     */
    fun getPublishedEvents(): List<PointBalanceEvent> {
        return publishedEvents.toList()
    }
    
    /**
     * 발행된 이벤트 개수 조회
     */
    fun getPublishedEventCount(): Int {
        return publishedEvents.size
    }
    
    /**
     * 특정 타입의 발행된 이벤트 조회
     */
    inline fun <reified T : PointBalanceEvent> getEventsOfType(): List<T> {
        return getPublishedEvents().filterIsInstance<T>()
    }
    
    /**
     * 마지막으로 발행된 이벤트 조회
     */
    fun getLastPublishedEvent(): PointBalanceEvent? {
        return publishedEvents.lastOrNull()
    }
    
    /**
     * 발행된 이벤트 초기화
     */
    fun clear() {
        publishedEvents.clear()
    }
}

