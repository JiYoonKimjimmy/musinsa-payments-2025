package com.musinsa.payments.point.infrastructure.persistence.jpa.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 포인트 사용 상세 JPA 엔티티
 * 도메인 엔티티 PointUsageDetail의 영속성 표현입니다.
 * 1원 단위 추적을 위한 상세 내역입니다.
 */
@Entity
@Table(
    name = "point_usage_detail",
    indexes = [
        Index(name = "idx_point_usage_id", columnList = "point_usage_id"),
        Index(name = "idx_point_accumulation_id", columnList = "point_accumulation_id")
    ]
)
class PointUsageDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "point_usage_id", nullable = false)
    var pointUsageId: Long = 0

    @Column(name = "point_accumulation_id", nullable = false)
    var pointAccumulationId: Long = 0

    @Column(name = "amount", nullable = false, precision = 10, scale = 0)
    var amount: BigDecimal = BigDecimal.ZERO

    @Column(name = "cancelled_amount", nullable = false, precision = 10, scale = 0)
    var cancelledAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}

