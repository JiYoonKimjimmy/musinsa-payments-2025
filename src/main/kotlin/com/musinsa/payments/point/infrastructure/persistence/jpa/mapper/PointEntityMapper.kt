package com.musinsa.payments.point.infrastructure.persistence.jpa.mapper

import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointConfig
import com.musinsa.payments.point.domain.entity.PointConfigHistory
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.entity.PointUsageDetail
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointAccumulationEntity
import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointConfigEntity
import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointConfigHistoryEntity
import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointUsageDetailEntity
import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointUsageEntity
import org.springframework.stereotype.Component

/**
 * 도메인 엔티티와 JPA 엔티티 간 변환을 담당하는 매퍼
 */
@Component
class PointEntityMapper {
    
    // ==================== PointAccumulation ====================
    
    /**
     * JPA 엔티티를 도메인 엔티티로 변환
     * 이미 저장된 엔티티이므로 만료일 검증을 우회하기 위해 restore 메서드를 사용합니다.
     */
    fun toDomain(entity: PointAccumulationEntity): PointAccumulation {
        return PointAccumulation.restore(
            id = entity.id,
            pointKey = entity.pointKey,
            memberId = entity.memberId,
            amount = Money.of(entity.amount),
            availableAmount = Money.of(entity.availableAmount),
            expirationDate = entity.expirationDate,
            isManualGrant = entity.isManualGrant,
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    /**
     * 도메인 엔티티를 JPA 엔티티로 변환
     */
    fun toEntity(domain: PointAccumulation): PointAccumulationEntity {
        val entity = PointAccumulationEntity()
        entity.id = domain.id
        entity.pointKey = domain.pointKey
        entity.memberId = domain.memberId
        entity.amount = domain.amount.toBigDecimal()
        entity.availableAmount = domain.availableAmount.toBigDecimal()
        entity.expirationDate = domain.expirationDate
        entity.isManualGrant = domain.isManualGrant
        entity.status = domain.status
        entity.createdAt = domain.createdAt
        entity.updatedAt = domain.updatedAt
        return entity
    }
    
    /**
     * JPA 엔티티 목록을 도메인 엔티티 목록으로 변환
     */
    fun toAccumulationDomainList(entities: List<PointAccumulationEntity>): List<PointAccumulation> {
        return entities.map { toDomain(it) }
    }
    
    // ==================== PointUsage ====================
    
    /**
     * JPA 엔티티를 도메인 엔티티로 변환
     */
    fun toDomain(entity: PointUsageEntity): PointUsage {
        val domain = PointUsage(
            pointKey = entity.pointKey,
            memberId = entity.memberId,
            orderNumber = OrderNumber.of(entity.orderNumber),
            totalAmount = Money.of(entity.totalAmount),
            cancelledAmount = Money.of(entity.cancelledAmount),
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
        domain.id = entity.id
        return domain
    }
    
    /**
     * 도메인 엔티티를 JPA 엔티티로 변환
     */
    fun toEntity(domain: PointUsage): PointUsageEntity {
        val entity = PointUsageEntity()
        entity.id = domain.id
        entity.pointKey = domain.pointKey
        entity.memberId = domain.memberId
        entity.orderNumber = domain.orderNumber.value
        entity.totalAmount = domain.totalAmount.toBigDecimal()
        entity.cancelledAmount = domain.cancelledAmount.toBigDecimal()
        entity.status = domain.status
        entity.createdAt = domain.createdAt
        entity.updatedAt = domain.updatedAt
        return entity
    }
    
    /**
     * JPA 엔티티 목록을 도메인 엔티티 목록으로 변환
     */
    fun toUsageDomainList(entities: List<PointUsageEntity>): List<PointUsage> {
        return entities.map { toDomain(it) }
    }
    
    // ==================== PointUsageDetail ====================
    
    /**
     * JPA 엔티티를 도메인 엔티티로 변환
     */
    fun toDomain(entity: PointUsageDetailEntity): PointUsageDetail {
        val domain = PointUsageDetail(
            pointUsageId = entity.pointUsageId,
            pointAccumulationId = entity.pointAccumulationId,
            amount = Money.of(entity.amount),
            cancelledAmount = Money.of(entity.cancelledAmount),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
        domain.id = entity.id
        return domain
    }
    
    /**
     * 도메인 엔티티를 JPA 엔티티로 변환
     */
    fun toEntity(domain: PointUsageDetail): PointUsageDetailEntity {
        val entity = PointUsageDetailEntity()
        entity.id = domain.id
        entity.pointUsageId = domain.pointUsageId
        entity.pointAccumulationId = domain.pointAccumulationId
        entity.amount = domain.amount.toBigDecimal()
        entity.cancelledAmount = domain.cancelledAmount.toBigDecimal()
        entity.createdAt = domain.createdAt
        entity.updatedAt = domain.updatedAt
        return entity
    }
    
    /**
     * JPA 엔티티 목록을 도메인 엔티티 목록으로 변환
     */
    fun toUsageDetailDomainList(entities: List<PointUsageDetailEntity>): List<PointUsageDetail> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * 도메인 엔티티 목록을 JPA 엔티티 목록으로 변환
     */
    fun toUsageDetailEntityList(domains: List<PointUsageDetail>): List<PointUsageDetailEntity> {
        return domains.map { toEntity(it) }
    }
    
    // ==================== PointConfig ====================
    
    /**
     * JPA 엔티티를 도메인 엔티티로 변환
     */
    fun toDomain(entity: PointConfigEntity): PointConfig {
        val domain = PointConfig(
            configKey = entity.configKey,
            configValue = entity.configValue,
            description = entity.description,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
        domain.id = entity.id
        return domain
    }
    
    /**
     * 도메인 엔티티를 JPA 엔티티로 변환
     */
    fun toEntity(domain: PointConfig): PointConfigEntity {
        val entity = PointConfigEntity()
        entity.id = domain.id
        entity.configKey = domain.configKey
        entity.configValue = domain.configValue
        entity.description = domain.description
        entity.createdAt = domain.createdAt
        entity.updatedAt = domain.updatedAt
        return entity
    }
    
    /**
     * JPA 엔티티 목록을 도메인 엔티티 목록으로 변환
     */
    fun toConfigDomainList(entities: List<PointConfigEntity>): List<PointConfig> {
        return entities.map { toDomain(it) }
    }
    
    // ==================== PointConfigHistory ====================
    
    /**
     * JPA 엔티티를 도메인 엔티티로 변환
     */
    fun toDomain(entity: PointConfigHistoryEntity): PointConfigHistory {
        val domain = PointConfigHistory(
            configKey = entity.configKey,
            newValue = entity.newValue,
            oldValue = entity.oldValue,
            changedBy = entity.changedBy,
            changedAt = entity.changedAt
        )
        domain.id = entity.id
        return domain
    }
    
    /**
     * 도메인 엔티티를 JPA 엔티티로 변환
     */
    fun toEntity(domain: PointConfigHistory): PointConfigHistoryEntity {
        val entity = PointConfigHistoryEntity()
        entity.id = domain.id
        entity.configKey = domain.configKey
        entity.oldValue = domain.oldValue
        entity.newValue = domain.newValue
        entity.changedBy = domain.changedBy
        entity.changedAt = domain.changedAt
        return entity
    }
    
    /**
     * JPA 엔티티 목록을 도메인 엔티티 목록으로 변환
     */
    fun toConfigHistoryDomainList(entities: List<PointConfigHistoryEntity>): List<PointConfigHistory> {
        return entities.map { toDomain(it) }
    }
}

