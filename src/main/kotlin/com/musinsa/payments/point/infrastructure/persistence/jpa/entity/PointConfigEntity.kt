package com.musinsa.payments.point.infrastructure.persistence.jpa.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 포인트 설정 JPA 엔티티
 * 도메인 엔티티 PointConfig의 영속성 표현입니다.
 */
@Entity
@Table(
    name = "point_config",
    indexes = [
        Index(name = "idx_config_key", columnList = "config_key", unique = true)
    ]
)
class PointConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "config_key", nullable = false, unique = true, length = 50)
    var configKey: String = ""

    @Column(name = "config_value", nullable = false, length = 255)
    var configValue: String = ""

    @Column(name = "description", length = 500)
    var description: String? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}

