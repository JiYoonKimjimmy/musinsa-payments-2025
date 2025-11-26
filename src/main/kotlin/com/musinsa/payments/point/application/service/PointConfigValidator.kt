package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.config.PointConfigPort
import com.musinsa.payments.point.domain.entity.PointConfig
import com.musinsa.payments.point.domain.exception.InvalidConfigKeyException
import com.musinsa.payments.point.domain.exception.InvalidConfigValueException
import org.springframework.stereotype.Service

/**
 * 포인트 설정 검증 서비스
 * 설정 값의 유효성을 검증하고 설정 간 의존성을 확인합니다.
 */
@Service
class PointConfigValidator(
    private val pointConfigPort: PointConfigPort
) {
    
    /**
     * 설정 값 검증
     * @param configKey 설정 키
     * @param configValue 설정 값
     * @throws InvalidConfigKeyException 설정 키가 유효하지 않은 경우
     * @throws InvalidConfigValueException 설정 값이 유효하지 않은 경우
     */
    fun validateConfigValue(configKey: String, configValue: String) {
        when (configKey) {
            "MAX_ACCUMULATION_AMOUNT_PER_TIME" -> {
                validateLongValue(configValue, 1L, Long.MAX_VALUE)
            }
            "MAX_BALANCE_PER_MEMBER" -> {
                validateLongValue(configValue, 1L, Long.MAX_VALUE)
            }
            "DEFAULT_EXPIRATION_DAYS" -> {
                validateIntegerValue(configValue, 1, 1824)
            }
            "MIN_EXPIRATION_DAYS" -> {
                validateIntegerValue(configValue, 1, 1824)
            }
            "MAX_EXPIRATION_DAYS" -> {
                validateIntegerValue(configValue, 1, 1824)
            }
            else -> {
                throw InvalidConfigKeyException("유효하지 않은 설정 키입니다: $configKey")
            }
        }
    }
    
    /**
     * 설정 간 의존성 검증
     * MIN_EXPIRATION_DAYS < DEFAULT_EXPIRATION_DAYS < MAX_EXPIRATION_DAYS
     * @throws InvalidConfigValueException 의존성 검증 실패 시
     */
    fun validateConfigDependencies() {
        val minDays = getConfig("MIN_EXPIRATION_DAYS")
        val maxDays = getConfig("MAX_EXPIRATION_DAYS")
        val defaultDays = getConfig("DEFAULT_EXPIRATION_DAYS")
        
        val minDaysValue = minDays.getIntValue()
        val maxDaysValue = maxDays.getIntValue()
        val defaultDaysValue = defaultDays.getIntValue()
        
        if (minDaysValue >= maxDaysValue) {
            throw InvalidConfigValueException("최소 만료일($minDaysValue)은 최대 만료일($maxDaysValue)보다 작아야 합니다.")
        }

        if (defaultDaysValue !in minDaysValue..<maxDaysValue) {
            throw InvalidConfigValueException("기본 만료일($defaultDaysValue)은 최소 만료일($minDaysValue) 이상, 최대 만료일($maxDaysValue) 미만이어야 합니다.")
        }
    }
    
    /**
     * Long 타입 설정 값 검증
     */
    private fun validateLongValue(value: String, min: Long, max: Long) {
        val longValue = value.toLongOrNull()
            ?: throw InvalidConfigValueException("설정 값은 숫자여야 합니다: $value")
        
        if (longValue !in min..max) {
            throw InvalidConfigValueException("설정 값은 $min 이상 $max 이하여야 합니다. (현재 값: $longValue)")
        }
    }
    
    /**
     * Integer 타입 설정 값 검증
     */
    private fun validateIntegerValue(value: String, min: Int, max: Int) {
        val intValue = value.toIntOrNull()
            ?: throw InvalidConfigValueException("설정 값은 숫자여야 합니다: $value")
        
        if (intValue !in min..max) {
            throw InvalidConfigValueException("설정 값은 $min 이상 $max 이하여야 합니다. (현재 값: $intValue)")
        }
    }
    
    /**
     * 설정 조회 헬퍼 메서드
     */
    private fun getConfig(configKey: String): PointConfig {
        return pointConfigPort.findByConfigKey(configKey)
            .orElseThrow { InvalidConfigValueException("설정을 찾을 수 없습니다: $configKey") }
    }
}

