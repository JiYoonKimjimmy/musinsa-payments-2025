package com.musinsa.payments.point.infrastructure.persistence.jpa.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 포인트 설정 변경 이력 JPA 엔티티
 * 도메인 엔티티 PointConfigHistory의 영속성 표현입니다.
 */
@Entity
@Table(
    name = "point_config_history",
    indexes = [
        Index(name = "idx_config_key", columnList = "config_key"),
        Index(name = "idx_changed_at", columnList = "changed_at")
    ]
)
class PointConfigHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "config_key", nullable = false, length = 50)
    var configKey: String = ""

    @Column(name = "old_value", length = 255)
    var oldValue: String? = null

    @Column(name = "new_value", nullable = false, length = 255)
    var newValue: String = ""

    @Column(name = "changed_by", length = 100)
    var changedBy: String? = null

    @Column(name = "changed_at", nullable = false)
    var changedAt: LocalDateTime = LocalDateTime.now()
}

