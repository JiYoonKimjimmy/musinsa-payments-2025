package com.musinsa.payments.point.infrastructure.persistence.jpa.repository

import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointUsageDetailEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 포인트 사용 상세 JPA 리포지토리
 */
@Repository
interface PointUsageDetailJpaRepository : JpaRepository<PointUsageDetailEntity, Long> {
    
    /**
     * 포인트 사용 ID로 조회
     */
    fun findByPointUsageId(pointUsageId: Long): List<PointUsageDetailEntity>
    
    /**
     * 포인트 적립 ID로 조회
     */
    fun findByPointAccumulationId(pointAccumulationId: Long): List<PointUsageDetailEntity>
    
    /**
     * 포인트 사용 키로 조회 (PointUsageEntity의 pointKey를 통해 조회)
     */
    @Query("""
        SELECT pud FROM PointUsageDetailEntity pud
        JOIN PointUsageEntity pu ON pud.pointUsageId = pu.id
        WHERE pu.pointKey = :pointKey
    """)
    fun findByUsagePointKey(@Param("pointKey") pointKey: String): List<PointUsageDetailEntity>
    
    /**
     * 포인트 적립 키로 조회 (PointAccumulationEntity의 pointKey를 통해 조회)
     */
    @Query("""
        SELECT pud FROM PointUsageDetailEntity pud
        JOIN PointAccumulationEntity pa ON pud.pointAccumulationId = pa.id
        WHERE pa.pointKey = :pointKey
    """)
    fun findByAccumulationPointKey(@Param("pointKey") pointKey: String): List<PointUsageDetailEntity>
}

