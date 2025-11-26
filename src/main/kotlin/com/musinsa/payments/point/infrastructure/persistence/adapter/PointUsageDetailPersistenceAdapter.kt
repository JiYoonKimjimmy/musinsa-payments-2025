package com.musinsa.payments.point.infrastructure.persistence.adapter

import com.musinsa.payments.point.application.port.output.persistence.PointUsageDetailPersistencePort
import com.musinsa.payments.point.domain.entity.PointUsageDetail
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointUsageDetailJpaRepository
import org.springframework.stereotype.Component

/**
 * 포인트 사용 상세 영속성 어댑터
 * Infrastructure 레이어에서 PointUsageDetailPersistencePort 인터페이스를 구현합니다.
 */
@Component
class PointUsageDetailPersistenceAdapter(
    private val pointUsageDetailJpaRepository: PointUsageDetailJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : PointUsageDetailPersistencePort {
    
    override fun saveAll(details: List<PointUsageDetail>): List<PointUsageDetail> {
        val entities = pointEntityMapper.toUsageDetailEntityList(details)
        val savedEntities = pointUsageDetailJpaRepository.saveAll(entities)
        return pointEntityMapper.toUsageDetailDomainList(savedEntities)
    }
    
    override fun findByUsagePointKey(pointKey: String): List<PointUsageDetail> {
        val entities = pointUsageDetailJpaRepository.findByUsagePointKey(pointKey)
        return pointEntityMapper.toUsageDetailDomainList(entities)
    }
    
    override fun findByAccumulationPointKey(pointKey: String): List<PointUsageDetail> {
        val entities = pointUsageDetailJpaRepository.findByAccumulationPointKey(pointKey)
        return pointEntityMapper.toUsageDetailDomainList(entities)
    }

    override fun findAll(): List<PointUsageDetail> {
        val entities = pointUsageDetailJpaRepository.findAll()
        return pointEntityMapper.toUsageDetailDomainList(entities)
    }

    override fun deleteById(id: Long) {
        pointUsageDetailJpaRepository.deleteById(id)
    }
}

