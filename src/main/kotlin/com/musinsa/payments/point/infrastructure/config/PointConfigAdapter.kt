package com.musinsa.payments.point.infrastructure.config

import com.musinsa.payments.point.application.port.output.config.PointConfigPort
import com.musinsa.payments.point.domain.entity.PointConfig
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointConfigJpaRepository
import org.springframework.stereotype.Component
import java.util.*

/**
 * 포인트 설정 어댑터
 * Infrastructure 레이어의 config 패키지에서 PointConfigPort 인터페이스를 구현합니다.
 */
@Component
class PointConfigAdapter(
    private val pointConfigJpaRepository: PointConfigJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : PointConfigPort {
    
    override fun findByConfigKey(configKey: String): Optional<PointConfig> {
        return pointConfigJpaRepository.findByConfigKey(configKey)
            .map { pointEntityMapper.toDomain(it) }
    }
    
    override fun findAll(): List<PointConfig> {
        val entities = pointConfigJpaRepository.findAll()
        return pointEntityMapper.toConfigDomainList(entities)
    }
    
    override fun save(config: PointConfig): PointConfig {
        val entity = pointEntityMapper.toEntity(config)
        val savedEntity = pointConfigJpaRepository.save(entity)
        return pointEntityMapper.toDomain(savedEntity)
    }
}

