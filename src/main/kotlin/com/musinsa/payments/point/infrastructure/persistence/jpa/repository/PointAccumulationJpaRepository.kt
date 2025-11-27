package com.musinsa.payments.point.infrastructure.persistence.jpa.repository

import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointAccumulationEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.*

/**
 * 포인트 적립 JPA 리포지토리
 */
@Repository
interface PointAccumulationJpaRepository : JpaRepository<PointAccumulationEntity, Long> {
    
    /**
     * 포인트 키로 조회
     */
    fun findByPointKey(pointKey: String): Optional<PointAccumulationEntity>
    
    /**
     * 회원 ID와 상태로 조회
     */
    fun findByMemberIdAndStatus(memberId: Long, status: PointAccumulationStatus): List<PointAccumulationEntity>
    
    /**
     * 회원 ID로 사용 가능한 적립 건 조회 (만료일 오름차순)
     * 상태가 ACCUMULATED이고 사용 가능 잔액이 0보다 큰 적립 건만 조회
     */
    @Query("""
        SELECT pa FROM PointAccumulationEntity pa
        WHERE pa.memberId = :memberId
          AND pa.status = 'ACCUMULATED'
          AND pa.availableAmount > 0
          AND pa.expirationDate >= CURRENT_DATE
        ORDER BY pa.isManualGrant DESC, pa.expirationDate ASC
    """)
    fun findAvailableAccumulationsByMemberId(@Param("memberId") memberId: Long): List<PointAccumulationEntity>
    
    /**
     * 회원 ID의 사용 가능 금액 합계 조회
     */
    @Query("""
        SELECT COALESCE(SUM(pa.availableAmount), 0) FROM PointAccumulationEntity pa
        WHERE pa.memberId = :memberId
          AND pa.status = 'ACCUMULATED'
          AND pa.availableAmount > 0
          AND pa.expirationDate >= CURRENT_DATE
    """)
    fun sumAvailableAmountByMemberId(@Param("memberId") memberId: Long): BigDecimal

    /**
     * ID로 조회 (비관적 쓰기 락)
     * SELECT ... FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pa FROM PointAccumulationEntity pa WHERE pa.id = :id")
    fun findByIdWithLock(@Param("id") id: Long): Optional<PointAccumulationEntity>

    /**
     * ID 목록으로 조회 (비관적 쓰기 락)
     * N+1 문제를 방지하기 위한 배치 조회 메서드입니다.
     * SELECT ... FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pa FROM PointAccumulationEntity pa WHERE pa.id IN :ids")
    fun findByIdsWithLock(@Param("ids") ids: List<Long>): List<PointAccumulationEntity>

    /**
     * 회원 ID로 사용 가능한 적립 건 조회 (비관적 쓰기 락)
     * 상태가 ACCUMULATED이고 사용 가능 잔액이 0보다 큰 적립 건만 조회
     * 수기 지급 우선, 만료일 짧은 순으로 정렬
     * SELECT ... FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT pa FROM PointAccumulationEntity pa
        WHERE pa.memberId = :memberId
          AND pa.status = 'ACCUMULATED'
          AND pa.availableAmount > 0
          AND pa.expirationDate >= CURRENT_DATE
        ORDER BY pa.isManualGrant DESC, pa.expirationDate ASC
    """)
    fun findAvailableAccumulationsByMemberIdWithLock(
        @Param("memberId") memberId: Long
    ): List<PointAccumulationEntity>
}

