package com.musinsa.payments.point.domain.entity

import java.time.LocalDateTime

/**
 * 포인트 설정 도메인 엔티티
 * 동적 설정 값을 관리합니다.
 */
class PointConfig {
    var id: Long? = null              // 엔티티 생성 시점에는 null, 저장 후에는 항상 값 존재
    var configKey: String             // 설정 키, 필수 (UNIQUE)
    var configValue: String           // 설정 값, 필수
    var description: String? = null   // 설명, 선택적
    var createdAt: LocalDateTime      // 생성일시, 필수
    var updatedAt: LocalDateTime      // 수정일시, 필수
    
    constructor(
        configKey: String,
        configValue: String,
        description: String? = null,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ) {
        require(configKey.isNotBlank()) { "설정 키는 필수입니다." }
        require(configValue.isNotBlank()) { "설정 값은 필수입니다." }
        
        this.configKey = configKey
        this.configValue = configValue
        this.description = description
        this.createdAt = createdAt
        this.updatedAt = updatedAt
    }
    
    /**
     * 설정 값을 Long 타입으로 변환
     */
    fun getLongValue(): Long {
        return configValue.toLongOrNull() 
            ?: throw IllegalArgumentException("configValue가 숫자가 아닙니다: $configValue")
    }
    
    /**
     * 설정 값을 Int 타입으로 변환
     */
    fun getIntValue(): Int {
        return configValue.toIntOrNull() 
            ?: throw IllegalArgumentException("configValue가 숫자가 아닙니다: $configValue")
    }
    
    /**
     * 설정 값을 Boolean 타입으로 변환
     */
    fun getBooleanValue(): Boolean {
        return configValue.toBoolean()
    }
    
    /**
     * 설정 값 업데이트
     * 설정 값을 변경하고 updatedAt을 자동으로 갱신합니다.
     */
    fun updateConfigValue(newValue: String) {
        require(newValue.isNotBlank()) { "설정 값은 필수입니다." }
        this.configValue = newValue
        this.updatedAt = LocalDateTime.now()
    }
}
