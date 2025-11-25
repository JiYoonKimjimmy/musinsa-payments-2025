package com.musinsa.payments.point.application.port.output.config.fixtures

import com.musinsa.payments.point.application.port.output.config.PointConfigPort
import com.musinsa.payments.point.domain.entity.PointConfig
import java.util.concurrent.atomic.AtomicLong

/**
 * 포인트 설정 포트의 Fake 구현체
 * 메모리 기반 설정 저장소를 사용하여 테스트에 활용합니다.
 */
class FakePointConfigPort : PointConfigPort {
    
    private val idGenerator = AtomicLong(1L)
    private val storage = mutableMapOf<String, PointConfig>()
    
    override fun findByConfigKey(configKey: String): java.util.Optional<PointConfig> {
        return java.util.Optional.ofNullable(storage[configKey])
    }
    
    override fun findAll(): List<PointConfig> {
        return storage.values.toList()
    }
    
    override fun save(config: PointConfig): PointConfig {
        // ID가 없으면 새로 할당
        if (config.id == null) {
            config.id = idGenerator.getAndIncrement()
        }
        
        // 저장
        storage[config.configKey] = config
        
        return config
    }
    
    /**
     * 기본 설정값 초기화
     * 테스트에서 자주 사용하는 기본 설정값들을 설정합니다.
     */
    fun setupDefaultConfigs() {
        save(PointConfig("MAX_ACCUMULATION_AMOUNT_PER_TIME", "100000"))
        save(PointConfig("MAX_BALANCE_PER_MEMBER", "10000000"))
        save(PointConfig("DEFAULT_EXPIRATION_DAYS", "365"))
        save(PointConfig("MIN_EXPIRATION_DAYS", "1"))
        save(PointConfig("MAX_EXPIRATION_DAYS", "1824"))
    }
    
    /**
     * 테스트 헬퍼: 저장소 초기화
     */
    fun clear() {
        storage.clear()
        idGenerator.set(1L)
    }
    
    /**
     * 테스트 헬퍼: 설정값 설정
     */
    fun setConfig(configKey: String, configValue: String) {
        val existing = storage[configKey]
        if (existing != null) {
            existing.updateConfigValue(configValue)
        } else {
            save(PointConfig(configKey, configValue))
        }
    }
    
    /**
     * 테스트 헬퍼: 여러 설정값을 한 번에 설정
     */
    fun setConfigs(vararg configs: Pair<String, String>) {
        configs.forEach { (key, value) ->
            setConfig(key, value)
        }
    }
    
    /**
     * 테스트 헬퍼: 기본 설정으로 리셋
     */
    fun resetToDefaults() {
        clear()
        setupDefaultConfigs()
    }
}

