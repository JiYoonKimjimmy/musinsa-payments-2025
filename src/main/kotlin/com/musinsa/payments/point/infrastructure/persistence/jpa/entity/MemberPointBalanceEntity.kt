package com.musinsa.payments.point.infrastructure.persistence.jpa.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 회원 포인트 잔액 JPA 엔티티
 * 도메인 엔티티 MemberPointBalance의 영속성 표현입니다.
 */
@Entity
@Table(name = "member_point_balance")
class MemberPointBalanceEntity {
    
    @Id
    @Column(name = "member_id")
    var memberId: Long = 0
    
    @Column(name = "available_balance", nullable = false, precision = 15, scale = 0)
    var availableBalance: BigDecimal = BigDecimal.ZERO
    
    @Column(name = "total_accumulated", nullable = false, precision = 15, scale = 0)
    var totalAccumulated: BigDecimal = BigDecimal.ZERO
    
    @Column(name = "total_used", nullable = false, precision = 15, scale = 0)
    var totalUsed: BigDecimal = BigDecimal.ZERO
    
    @Column(name = "total_expired", nullable = false, precision = 15, scale = 0)
    var totalExpired: BigDecimal = BigDecimal.ZERO
    
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
    
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}

