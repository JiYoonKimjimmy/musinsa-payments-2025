package com.musinsa.payments.point.infrastructure.persistence.adapter

import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointUsageJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.*

/**
 * 포인트 사용 영속성 어댑터
 * Infrastructure 레이어에서 PointUsagePersistencePort 인터페이스를 구현합니다.
 */
@Component
class PointUsagePersistenceAdapter(
    private val pointUsageJpaRepository: PointUsageJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : PointUsagePersistencePort {
    
    override fun save(usage: PointUsage): PointUsage {
        val entity = pointEntityMapper.toEntity(usage)
        val savedEntity = pointUsageJpaRepository.save(entity)
        return pointEntityMapper.toDomain(savedEntity)
    }
    
    override fun findByPointKey(pointKey: String): Optional<PointUsage> {
        return pointUsageJpaRepository.findByPointKey(pointKey)
            .map { pointEntityMapper.toDomain(it) }
    }
    
    override fun findByMemberIdAndOrderNumber(
        memberId: Long,
        orderNumber: OrderNumber
    ): List<PointUsage> {
        val entities = pointUsageJpaRepository.findByMemberIdAndOrderNumber(memberId, orderNumber.value)
        return pointEntityMapper.toUsageDomainList(entities)
    }
    
    override fun findUsageHistoryByMemberId(
        memberId: Long,
        orderNumber: String?,
        pageable: Pageable
    ): Page<PointUsage> {
        val entityPage = pointUsageJpaRepository.findByMemberId(memberId, orderNumber, pageable)
        val domainList = pointEntityMapper.toUsageDomainList(entityPage.content)
        return PageImpl(domainList, pageable, entityPage.totalElements)
    }
}

