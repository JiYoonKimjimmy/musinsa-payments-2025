package com.musinsa.payments.point.infrastructure.persistence.jpa.repository

import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointConfigHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 포인트 설정 변경 이력 JPA 리포지토리
 */
@Repository
interface PointConfigHistoryJpaRepository : JpaRepository<PointConfigHistoryEntity, Long> {
    
    /**
     * 설정 키로 변경 이력 조회
     * @param configKey 설정 키
     * @return 변경 이력 목록 (변경일시 내림차순)
     */
    fun findByConfigKeyOrderByChangedAtDesc(configKey: String): List<PointConfigHistoryEntity>
}

