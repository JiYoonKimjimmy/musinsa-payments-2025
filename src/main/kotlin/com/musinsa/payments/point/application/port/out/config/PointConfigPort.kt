package com.musinsa.payments.point.application.port.out.config

import com.musinsa.payments.point.domain.entity.PointConfig
import java.util.*

/**
 * 포인트 설정 포트 인터페이스
 * Application 레이어에서 정의하는 아웃바운드 포트입니다.
 * Infrastructure 레이어에서 이 인터페이스를 구현합니다.
 */
interface PointConfigPort {
    
    /**
     * 설정 키로 조회
     * @param configKey 설정 키
     * @return 포인트 설정 엔티티 (없으면 empty)
     */
    fun findByConfigKey(configKey: String): Optional<PointConfig>
    
    /**
     * 모든 설정 조회
     * @return 포인트 설정 엔티티 목록
     */
    fun findAll(): List<PointConfig>
    
    /**
     * 설정 저장
     * @param config 저장할 포인트 설정 엔티티
     * @return 저장된 포인트 설정 엔티티 (id 포함)
     */
    fun save(config: PointConfig): PointConfig
}

