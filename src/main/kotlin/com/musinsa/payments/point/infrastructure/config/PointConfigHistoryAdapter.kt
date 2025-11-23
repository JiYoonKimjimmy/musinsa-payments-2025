package com.musinsa.payments.point.infrastructure.config

import com.musinsa.payments.point.application.port.output.config.PointConfigHistoryPort
import com.musinsa.payments.point.domain.entity.PointConfigHistory
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointConfigHistoryJpaRepository
import org.springframework.stereotype.Component

/**
 * 포인트 설정 변경 이력 어댑터
 * Infrastructure 레이어의 config 패키지에서 PointConfigHistoryPort 인터페이스를 구현합니다.
 */
@Component
class PointConfigHistoryAdapter(
    private val pointConfigHistoryJpaRepository: PointConfigHistoryJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : PointConfigHistoryPort {
    
    override fun save(history: PointConfigHistory): PointConfigHistory {
        val entity = pointEntityMapper.toEntity(history)
        val savedEntity = pointConfigHistoryJpaRepository.save(entity)
        return pointEntityMapper.toDomain(savedEntity)
    }
    
    override fun findByConfigKey(configKey: String): List<PointConfigHistory> {
        val entities = pointConfigHistoryJpaRepository.findByConfigKeyOrderByChangedAtDesc(configKey)
        return pointEntityMapper.toConfigHistoryDomainList(entities)
    }
}

