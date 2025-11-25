package com.musinsa.payments.point.infrastructure.persistence.jpa.repository

import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointUsageEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * 포인트 사용 JPA 리포지토리
 */
@Repository
interface PointUsageJpaRepository : JpaRepository<PointUsageEntity, Long> {
    
    /**
     * 포인트 키로 조회
     */
    fun findByPointKey(pointKey: String): Optional<PointUsageEntity>
    
    /**
     * 회원 ID와 주문번호로 조회
     */
    fun findByMemberIdAndOrderNumber(memberId: Long, orderNumber: String): List<PointUsageEntity>
    
    /**
     * 회원 ID로 사용 내역 조회 (페이징)
     * 주문번호 필터링 옵션 포함
     */
    @Query("""
        SELECT pu FROM PointUsageEntity pu
        WHERE pu.memberId = :memberId
          AND (:orderNumber IS NULL OR pu.orderNumber = :orderNumber)
        ORDER BY pu.createdAt DESC, pu.id DESC
    """)
    fun findByMemberId(
        @Param("memberId") memberId: Long,
        @Param("orderNumber") orderNumber: String?,
        pageable: Pageable
    ): Page<PointUsageEntity>
}

