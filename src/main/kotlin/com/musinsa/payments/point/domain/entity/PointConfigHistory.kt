package com.musinsa.payments.point.domain.entity

import java.time.LocalDateTime

/**
 * 포인트 설정 변경 이력 도메인 엔티티
 * 설정 변경 시 이력을 기록합니다.
 */
class PointConfigHistory {
    var id: Long? = null              // 엔티티 생성 시점에는 null, 저장 후에는 항상 값 존재
    var configKey: String             // 설정 키, 필수
    var oldValue: String? = null       // 이전 값, 선택적
    var newValue: String              // 새로운 값, 필수
    var changedBy: String? = null     // 변경한 사용자, 선택적
    var changedAt: LocalDateTime      // 변경일시, 필수
    
    constructor(
        configKey: String,
        newValue: String,
        oldValue: String? = null,
        changedBy: String? = null,
        changedAt: LocalDateTime = LocalDateTime.now()
    ) {
        require(configKey.isNotBlank()) { "설정 키는 필수입니다." }
        require(newValue.isNotBlank()) { "새로운 값은 필수입니다." }
        
        this.configKey = configKey
        this.oldValue = oldValue
        this.newValue = newValue
        this.changedBy = changedBy
        this.changedAt = changedAt
    }
}

