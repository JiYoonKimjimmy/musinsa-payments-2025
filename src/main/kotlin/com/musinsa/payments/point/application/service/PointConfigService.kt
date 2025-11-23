package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.config.PointConfigPort
import com.musinsa.payments.point.domain.entity.PointConfig
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
    private val pointConfigPort: PointConfigPort
) {
    
    /**
     * 설정 키로 조회
     * @param configKey 설정 키
     * @return 포인트 설정 엔티티 (없으면 empty)
     */
    fun findByConfigKey(configKey: String): Optional<PointConfig> {
        return pointConfigPort.findByConfigKey(configKey)
    }
    
    /**
     * 모든 설정 조회
     * @return 포인트 설정 엔티티 목록
     */
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
}

