package com.musinsa.payments.point.infrastructure.persistence.adapter

import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointAccumulationJpaRepository
import org.springframework.stereotype.Component
import java.util.*

/**
 * 포인트 적립 영속성 어댑터
 * Infrastructure 레이어에서 PointAccumulationPersistencePort 인터페이스를 구현합니다.
 */
@Component
class PointAccumulationPersistenceAdapter(
    private val pointAccumulationJpaRepository: PointAccumulationJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : PointAccumulationPersistencePort {
    
    override fun save(accumulation: PointAccumulation): PointAccumulation {
        val entity = pointEntityMapper.toEntity(accumulation)
        val savedEntity = pointAccumulationJpaRepository.save(entity)
        return pointEntityMapper.toDomain(savedEntity)
    }
    
    override fun saveAll(accumulations: List<PointAccumulation>): List<PointAccumulation> {
        val entities = accumulations.map { pointEntityMapper.toEntity(it) }
        val savedEntities = pointAccumulationJpaRepository.saveAll(entities)
        return pointEntityMapper.toAccumulationDomainList(savedEntities)
    }
    
    override fun findById(id: Long): Optional<PointAccumulation> {
        return pointAccumulationJpaRepository.findById(id)
            .map { pointEntityMapper.toDomain(it) }
    }
    
    override fun findByPointKey(pointKey: String): Optional<PointAccumulation> {
        return pointAccumulationJpaRepository.findByPointKey(pointKey)
            .map { pointEntityMapper.toDomain(it) }
    }
    
    override fun findByMemberIdAndStatus(
        memberId: Long,
        status: PointAccumulationStatus
    ): List<PointAccumulation> {
        val entities = pointAccumulationJpaRepository.findByMemberIdAndStatus(memberId, status)
        return pointEntityMapper.toAccumulationDomainList(entities)
    }
    
    override fun findAvailableAccumulationsByMemberId(memberId: Long): List<PointAccumulation> {
        val entities = pointAccumulationJpaRepository.findAvailableAccumulationsByMemberId(memberId)
        return pointEntityMapper.toAccumulationDomainList(entities)
    }
    
    override fun sumAvailableAmountByMemberId(memberId: Long): Money {
        val sum = pointAccumulationJpaRepository.sumAvailableAmountByMemberId(memberId)
        return Money.of(sum)
    }

    override fun findByIdWithLock(id: Long): Optional<PointAccumulation> {
        return pointAccumulationJpaRepository.findByIdWithLock(id)
            .map { pointEntityMapper.toDomain(it) }
    }

    override fun findByIdsWithLock(ids: List<Long>): Map<Long, PointAccumulation> {
        if (ids.isEmpty()) {
            return emptyMap()
        }
        val entities = pointAccumulationJpaRepository.findByIdsWithLock(ids)
        val domainList = pointEntityMapper.toAccumulationDomainList(entities)
        return domainList.associateBy { it.id!! }
    }

    override fun findAvailableAccumulationsByMemberIdWithLock(memberId: Long): List<PointAccumulation> {
        val entities = pointAccumulationJpaRepository.findAvailableAccumulationsByMemberIdWithLock(memberId)
        return pointEntityMapper.toAccumulationDomainList(entities)
    }

    override fun findAll(): List<PointAccumulation> {
        val entities = pointAccumulationJpaRepository.findAll()
        return pointEntityMapper.toAccumulationDomainList(entities)
    }

    override fun deleteById(id: Long) {
        pointAccumulationJpaRepository.deleteById(id)
    }
}

