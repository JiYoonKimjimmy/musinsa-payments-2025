package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.config.PointConfigHistoryPort
import com.musinsa.payments.point.application.port.output.config.PointConfigPort
import com.musinsa.payments.point.domain.entity.PointConfig
import com.musinsa.payments.point.domain.entity.PointConfigHistory
import com.musinsa.payments.point.domain.exception.ConfigNotFoundException
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * 포인트 설정 서비스
 * 설정 조회 및 관리 로직을 담당합니다.
 */
@Transactional(readOnly = true)
@Service
class PointConfigService(
    private val pointConfigPort: PointConfigPort,
    private val pointConfigValidator: PointConfigValidator,
    private val pointConfigHistoryPort: PointConfigHistoryPort
) {
    
    /**
     * 설정 키로 조회
     * @param configKey 설정 키
     * @return 포인트 설정 엔티티 (없으면 empty)
     */
    @Cacheable(value = ["pointConfig"], key = "#configKey")
    fun findByConfigKey(configKey: String): Optional<PointConfig> {
        return pointConfigPort.findByConfigKey(configKey)
    }
    
    /**
     * 모든 설정 조회
     * @return 포인트 설정 엔티티 목록
     */
    @Cacheable(value = ["pointConfig"], key = "'all'")
    fun findAll(): List<PointConfig> {
        return pointConfigPort.findAll()
    }
    
    /**
     * 설정 값을 Long 타입으로 조회
     * @param configKey 설정 키
     * @return 설정 값 (Long)
     * @throws IllegalArgumentException 설정을 찾을 수 없거나 변환할 수 없는 경우
     */
    fun getLongValue(configKey: String): Long {
        return pointConfigPort.findByConfigKey(configKey)
            .orElseThrow { IllegalArgumentException("설정을 찾을 수 없습니다: $configKey") }
            .getLongValue()
    }
    
    /**
     * 설정 값을 Int 타입으로 조회
     * @param configKey 설정 키
     * @return 설정 값 (Int)
     * @throws IllegalArgumentException 설정을 찾을 수 없거나 변환할 수 없는 경우
     */
    fun getIntValue(configKey: String): Int {
        return pointConfigPort.findByConfigKey(configKey)
            .orElseThrow { IllegalArgumentException("설정을 찾을 수 없습니다: $configKey") }
            .getIntValue()
    }
    
    /**
     * 설정 값을 Boolean 타입으로 조회
     * @param configKey 설정 키
     * @return 설정 값 (Boolean)
     * @throws IllegalArgumentException 설정을 찾을 수 없는 경우
     */
    fun getBooleanValue(configKey: String): Boolean {
        return pointConfigPort.findByConfigKey(configKey)
            .orElseThrow { IllegalArgumentException("설정을 찾을 수 없습니다: $configKey") }
            .getBooleanValue()
    }
    
    /**
     * 설정 업데이트
     * 설정 값을 변경하고 변경 이력을 기록합니다.
     * @param configKey 설정 키
     * @param configValue 새로운 설정 값
     * @param description 설명 (선택적)
     * @param changedBy 변경한 사용자 (선택적)
     * @return 업데이트된 포인트 설정 엔티티
     * @throws ConfigNotFoundException 설정을 찾을 수 없는 경우
     * @throws InvalidConfigValueException 설정 값이 유효하지 않은 경우
     */
    @CacheEvict(value = ["pointConfig"], allEntries = true)
    @Transactional
    fun updateConfig(
        configKey: String,
        configValue: String,
        description: String? = null,
        changedBy: String? = null
    ): PointConfig {
        // 설정 조회
        val config = pointConfigPort.findByConfigKey(configKey)
            .orElseThrow { ConfigNotFoundException("설정을 찾을 수 없습니다: $configKey") }
        
        // 이전 값 저장
        val oldValue = config.configValue
        
        // 설정 값 검증
        pointConfigValidator.validateConfigValue(configKey, configValue)
        
        // 설정 업데이트
        config.updateConfigValue(configValue)
        if (description != null) {
            config.description = description
        }
        
        // 저장
        val updatedConfig = pointConfigPort.save(config)
        
        // 변경 이력 기록
        val history = PointConfigHistory(
            configKey = configKey,
            newValue = configValue,
            oldValue = oldValue,
            changedBy = changedBy
        )
        pointConfigHistoryPort.save(history)
        
        // 설정 간 의존성 검증 (만료일 관련 설정인 경우)
        if (configKey.contains("EXPIRATION_DAYS")) {
            pointConfigValidator.validateConfigDependencies()
        }
        
        return updatedConfig
    }
}

