package com.musinsa.payments.point.application.port.output.config

import com.musinsa.payments.point.domain.entity.PointConfigHistory

/**
 * 포인트 설정 변경 이력 포트
 * Application 레이어에서 설정 변경 이력을 저장하기 위한 아웃바운드 포트입니다.
 */
interface PointConfigHistoryPort {
    
    /**
     * 설정 변경 이력 저장
     * @param history 변경 이력 도메인 엔티티
     * @return 저장된 변경 이력 도메인 엔티티
     */
    fun save(history: PointConfigHistory): PointConfigHistory
    
    /**
     * 설정 키로 변경 이력 조회
     * @param configKey 설정 키
     * @return 변경 이력 목록 (변경일시 내림차순)
     */
    fun findByConfigKey(configKey: String): List<PointConfigHistory>
}

