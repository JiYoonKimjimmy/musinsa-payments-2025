package com.musinsa.payments.point.infrastructure.config

import com.musinsa.payments.point.domain.entity.PointConfig
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointConfigJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 데이터베이스 초기화 컴포넌트
 * 애플리케이션 시작 시 기본 설정 데이터를 초기화합니다.
 */
@Component
class DataInitializer(
    private val pointConfigJpaRepository: PointConfigJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : CommandLineRunner {
    
    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)
    
    companion object {
        // 기본 설정 키 목록
        private const val MAX_ACCUMULATION_AMOUNT_PER_TIME = "MAX_ACCUMULATION_AMOUNT_PER_TIME"
        private const val MAX_BALANCE_PER_MEMBER = "MAX_BALANCE_PER_MEMBER"
        private const val DEFAULT_EXPIRATION_DAYS = "DEFAULT_EXPIRATION_DAYS"
        private const val MIN_EXPIRATION_DAYS = "MIN_EXPIRATION_DAYS"
        private const val MAX_EXPIRATION_DAYS = "MAX_EXPIRATION_DAYS"
        
        // 기본 설정 값
        private const val MAX_ACCUMULATION_AMOUNT_PER_TIME_VALUE = "100000"
        private const val MAX_BALANCE_PER_MEMBER_VALUE = "10000000"
        private const val DEFAULT_EXPIRATION_DAYS_VALUE = "365"
        private const val MIN_EXPIRATION_DAYS_VALUE = "1"
        private const val MAX_EXPIRATION_DAYS_VALUE = "1824"
        
        // 기본 설정 설명
        private const val MAX_ACCUMULATION_AMOUNT_PER_TIME_DESC = "1회 최대 적립 금액"
        private const val MAX_BALANCE_PER_MEMBER_DESC = "개인별 최대 보유 금액"
        private const val DEFAULT_EXPIRATION_DAYS_DESC = "기본 만료일 (일)"
        private const val MIN_EXPIRATION_DAYS_DESC = "최소 만료일 (일)"
        private const val MAX_EXPIRATION_DAYS_DESC = "최대 만료일 (일, 약 5년)"
    }
    
    override fun run(vararg args: String?) {
        logger.info("데이터베이스 초기화를 시작합니다...")
        
        initializeConfigs()
        
        logger.info("데이터베이스 초기화를 완료했습니다.")
    }
    
    /**
     * 기본 설정 데이터 초기화
     * 이미 존재하는 설정은 건너뛰고, 존재하지 않는 설정만 생성합니다.
     */
    private fun initializeConfigs() {
        val now = LocalDateTime.now()
        
        // 초기화할 설정 목록
        val configsToInitialize = listOf(
            ConfigData(
                key = MAX_ACCUMULATION_AMOUNT_PER_TIME,
                value = MAX_ACCUMULATION_AMOUNT_PER_TIME_VALUE,
                description = MAX_ACCUMULATION_AMOUNT_PER_TIME_DESC
            ),
            ConfigData(
                key = MAX_BALANCE_PER_MEMBER,
                value = MAX_BALANCE_PER_MEMBER_VALUE,
                description = MAX_BALANCE_PER_MEMBER_DESC
            ),
            ConfigData(
                key = DEFAULT_EXPIRATION_DAYS,
                value = DEFAULT_EXPIRATION_DAYS_VALUE,
                description = DEFAULT_EXPIRATION_DAYS_DESC
            ),
            ConfigData(
                key = MIN_EXPIRATION_DAYS,
                value = MIN_EXPIRATION_DAYS_VALUE,
                description = MIN_EXPIRATION_DAYS_DESC
            ),
            ConfigData(
                key = MAX_EXPIRATION_DAYS,
                value = MAX_EXPIRATION_DAYS_VALUE,
                description = MAX_EXPIRATION_DAYS_DESC
            )
        )
        
        var initializedCount = 0
        var skippedCount = 0
        
        for (configData in configsToInitialize) {
            // 이미 존재하는 설정인지 확인
            val existingConfig = pointConfigJpaRepository.findByConfigKey(configData.key)
            
            if (existingConfig.isPresent) {
                logger.debug("설정 '{}'는 이미 존재하므로 건너뜁니다.", configData.key)
                skippedCount++
                continue
            }
            
            // 도메인 엔티티 생성
            val domainConfig = PointConfig(
                configKey = configData.key,
                configValue = configData.value,
                description = configData.description,
                createdAt = now,
                updatedAt = now
            )
            
            // JPA 엔티티로 변환하여 저장
            val entity = pointEntityMapper.toEntity(domainConfig)
            pointConfigJpaRepository.save(entity)
            
            logger.info("설정 '{}' (값: '{}', 설명: '{}') 초기화 완료", 
                configData.key, configData.value, configData.description ?: "")
            initializedCount++
        }
        
        logger.info("설정 초기화 완료: 신규 생성 {}개, 기존 유지 {}개", 
            initializedCount, skippedCount)
    }
    
    /**
     * 설정 데이터 보유 클래스
     */
    private data class ConfigData(
        val key: String,
        val value: String,
        val description: String?
    )
}

