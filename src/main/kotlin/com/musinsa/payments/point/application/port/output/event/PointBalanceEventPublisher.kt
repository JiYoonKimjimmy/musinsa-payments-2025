package com.musinsa.payments.point.application.port.output.event

import com.musinsa.payments.point.domain.event.PointBalanceEvent

/**
 * 포인트 잔액 이벤트 발행 포트 인터페이스
 * Application 레이어에서 정의하는 아웃바운드 포트입니다.
 * Infrastructure 레이어에서 이 인터페이스를 구현합니다.
 */
interface PointBalanceEventPublisher {
    
    /**
     * 포인트 잔액 변경 이벤트 발행
     * @param event 발행할 이벤트
     */
    fun publish(event: PointBalanceEvent)
}

