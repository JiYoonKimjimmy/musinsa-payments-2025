package com.musinsa.payments.point.infrastructure.persistence.jpa.repository

import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointConfigEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

/**
 * 포인트 설정 JPA 리포지토리
 */
@Repository
interface PointConfigJpaRepository : JpaRepository<PointConfigEntity, Long> {
    
    /**
     * 설정 키로 조회
     */
    fun findByConfigKey(configKey: String): Optional<PointConfigEntity>
    
    // findAll() 메서드는 JpaRepository에 이미 존재하므로 별도 선언 불필요
}

