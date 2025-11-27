package com.musinsa.payments.point.application.port.output.event.fixtures

import org.springframework.context.ApplicationEventPublisher

/**
 * 테스트용 No-Op 이벤트 발행자
 * Spring의 ApplicationEventPublisher를 구현하지만 이벤트를 무시합니다.
 * 이벤트 검증이 필요하지 않은 테스트에서 사용합니다.
 */
class NoOpEventPublisher : ApplicationEventPublisher {
    override fun publishEvent(event: Any) {
        // 테스트에서는 이벤트를 무시
    }
}

