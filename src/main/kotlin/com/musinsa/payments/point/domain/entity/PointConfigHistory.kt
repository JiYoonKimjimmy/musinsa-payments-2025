package com.musinsa.payments.point.domain.entity

import java.time.LocalDateTime

/**
 * 포인트 설정 변경 이력 도메인 엔티티
 * 설정 변경 시 이력을 기록합니다.
 */
class PointConfigHistory(
    var id: Long? = null,                                  // 엔티티 생성 시점에는 null, 저장 후에는 항상 값 존재
    val configKey: String,                                  // 설정 키, 필수
    val oldValue: String? = null,                          // 이전 값, 선택적
    val newValue: String,                                  // 새로운 값, 필수
    val changedBy: String? = null,                         // 변경한 사용자, 선택적
    val changedAt: LocalDateTime = LocalDateTime.now(),    // 변경일시, 필수
) {
    init {
        require(configKey.isNotBlank()) { "설정 키는 필수입니다." }
        require(newValue.isNotBlank()) { "새로운 값은 필수입니다." }
    }
}

