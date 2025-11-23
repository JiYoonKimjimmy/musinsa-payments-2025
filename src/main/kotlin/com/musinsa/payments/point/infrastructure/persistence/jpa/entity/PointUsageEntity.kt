package com.musinsa.payments.point.infrastructure.persistence.jpa.entity

import com.musinsa.payments.point.domain.entity.PointUsageStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 포인트 사용 JPA 엔티티
 * 도메인 엔티티 PointUsage의 영속성 표현입니다.
 */
@Entity
@Table(
    name = "point_usage",
    indexes = [
        Index(name = "idx_member_order", columnList = "member_id,order_number"),
        Index(name = "idx_order_number", columnList = "order_number"),
        Index(name = "idx_point_key", columnList = "point_key", unique = true)
    ]
)
class PointUsageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "point_key", nullable = false, unique = true, length = 50)
    var pointKey: String = ""

    @Column(name = "member_id", nullable = false)
    var memberId: Long = 0

    @Column(name = "order_number", nullable = false, length = 50)
    var orderNumber: String = ""

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 0)
    var totalAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "cancelled_amount", nullable = false, precision = 10, scale = 0)
    var cancelledAmount: BigDecimal = BigDecimal.ZERO

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PointUsageStatus = PointUsageStatus.USED

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}

