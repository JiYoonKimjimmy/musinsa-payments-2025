package com.musinsa.payments.point.infrastructure.persistence.jpa.repository

import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.MemberPointBalanceEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * 회원 포인트 잔액 JPA 리포지토리
 */
@Repository
interface MemberPointBalanceJpaRepository : JpaRepository<MemberPointBalanceEntity, Long> {
    
    /**
     * 회원 ID로 조회
     */
    fun findByMemberId(memberId: Long): Optional<MemberPointBalanceEntity>
    
    /**
     * 회원 ID로 조회 (비관적 쓰기 락)
     * SELECT ... FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM MemberPointBalanceEntity b WHERE b.memberId = :memberId")
    fun findByMemberIdWithLock(@Param("memberId") memberId: Long): Optional<MemberPointBalanceEntity>
    
    /**
     * 회원 ID 목록으로 조회
     */
    fun findByMemberIdIn(memberIds: List<Long>): List<MemberPointBalanceEntity>
}

