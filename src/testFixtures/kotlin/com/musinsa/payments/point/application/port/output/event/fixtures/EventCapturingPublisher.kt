package com.musinsa.payments.point.application.port.output.event.fixtures

import org.springframework.context.ApplicationEventPublisher

/**
 * 테스트용 이벤트 캡처 발행자
 * Spring의 ApplicationEventPublisher를 구현하여 발행된 이벤트를 캡처하고 검증할 수 있습니다.
 */
class EventCapturingPublisher : ApplicationEventPublisher {
    val capturedEvents = mutableListOf<Any>()
    
    override fun publishEvent(event: Any) {
        capturedEvents.add(event)
    }
    
    /**
     * 캡처된 이벤트 초기화
     */
    fun clear() {
        capturedEvents.clear()
    }
    
    /**
     * 특정 타입의 캡처된 이벤트 조회
     */
    inline fun <reified T> getEventsOfType(): List<T> {
        return capturedEvents.filterIsInstance<T>()
    }
    
    /**
     * 마지막으로 캡처된 이벤트 조회
     */
    fun getLastCapturedEvent(): Any? {
        return capturedEvents.lastOrNull()
    }
}

