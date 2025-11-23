package com.musinsa.payments.point.infrastructure.persistence.jpa.entity

import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 포인트 적립 JPA 엔티티
 * 도메인 엔티티 PointAccumulation의 영속성 표현입니다.
 */
@Entity
@Table(
    name = "point_accumulation",
    indexes = [
        Index(name = "idx_member_status_expiration", columnList = "member_id,status,expiration_date"),
        Index(name = "idx_manual_expiration", columnList = "is_manual_grant,expiration_date"),
        Index(name = "idx_point_key", columnList = "point_key", unique = true)
    ]
)
class PointAccumulationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "point_key", nullable = false, unique = true, length = 50)
    var pointKey: String = ""

    @Column(name = "member_id", nullable = false)
    var memberId: Long = 0

    @Column(name = "amount", nullable = false, precision = 10, scale = 0)
    var amount: BigDecimal = BigDecimal.ZERO

    @Column(name = "available_amount", nullable = false, precision = 10, scale = 0)
    var availableAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "expiration_date", nullable = false)
    var expirationDate: LocalDate = LocalDate.now()

    @Column(name = "is_manual_grant", nullable = false)
    var isManualGrant: Boolean = false

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PointAccumulationStatus = PointAccumulationStatus.ACCUMULATED

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}

