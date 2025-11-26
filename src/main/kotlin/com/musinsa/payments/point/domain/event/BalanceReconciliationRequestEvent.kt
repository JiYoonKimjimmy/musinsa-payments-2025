package com.musinsa.payments.point.domain.event

import java.time.LocalDateTime

/**
 * 잔액 보정 요청 이벤트
 * 캐시 업데이트 실패 시 발행되어 비동기로 잔액 보정을 수행합니다.
 * 
 * @property memberId 보정 대상 회원 ID
 * @property reason 보정 요청 사유 (실패 원인)
 * @property originalEventType 원본 이벤트 타입 (적립, 사용, 취소 등)
 * @property occurredAt 이벤트 발생 시각
 */
data class BalanceReconciliationRequestEvent(
    val memberId: Long,
    val reason: String,
    val originalEventType: String,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)

