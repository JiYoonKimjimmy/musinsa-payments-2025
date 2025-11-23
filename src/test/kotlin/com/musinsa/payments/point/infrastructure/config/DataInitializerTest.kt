package com.musinsa.payments.point.infrastructure.config

import com.musinsa.payments.point.domain.entity.PointConfig
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointConfigJpaRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * DataInitializer 통합 테스트
 * @DataJpaTest를 사용하여 실제 테스트 DB에 데이터 저장을 확인합니다.
 * 테스트 설정은 src/test/resources/application.yml에서 관리합니다.
 */
@Import(PointEntityMapper::class)
@ActiveProfiles("test")
@DataJpaTest
class DataInitializerTest @Autowired constructor(
    private val pointConfigJpaRepository: PointConfigJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : StringSpec({
    
    extensions(SpringExtension)
    
    beforeTest {
        pointConfigJpaRepository.deleteAll()
    }
    
    "DataInitializer는 애플리케이션 시작 시 기본 설정 데이터를 초기화해야 한다" {
        // given
        val dataInitializer = DataInitializer(pointConfigJpaRepository, pointEntityMapper)
        
        // when
        dataInitializer.run()
        
        // then - 모든 기본 설정이 저장되었는지 확인
        val maxAccumulationAmount = pointConfigJpaRepository.findByConfigKey("MAX_ACCUMULATION_AMOUNT_PER_TIME")
        val maxBalance = pointConfigJpaRepository.findByConfigKey("MAX_BALANCE_PER_MEMBER")
        val defaultExpirationDays = pointConfigJpaRepository.findByConfigKey("DEFAULT_EXPIRATION_DAYS")
        val minExpirationDays = pointConfigJpaRepository.findByConfigKey("MIN_EXPIRATION_DAYS")
        val maxExpirationDays = pointConfigJpaRepository.findByConfigKey("MAX_EXPIRATION_DAYS")
        
        maxAccumulationAmount.isPresent shouldBe true
        maxBalance.isPresent shouldBe true
        defaultExpirationDays.isPresent shouldBe true
        minExpirationDays.isPresent shouldBe true
        maxExpirationDays.isPresent shouldBe true
    }
    
    "DataInitializer는 MAX_ACCUMULATION_AMOUNT_PER_TIME 설정을 올바른 값으로 초기화해야 한다" {
        // given
        val dataInitializer = DataInitializer(pointConfigJpaRepository, pointEntityMapper)
        
        // when
        dataInitializer.run()
        
        // then
        val config = pointConfigJpaRepository.findByConfigKey("MAX_ACCUMULATION_AMOUNT_PER_TIME")
        config.isPresent shouldBe true
        val domainConfig = pointEntityMapper.toDomain(config.get())
        domainConfig.configValue shouldBe "100000"
        domainConfig.description shouldBe "1회 최대 적립 금액"
    }
    
    "DataInitializer는 MAX_BALANCE_PER_MEMBER 설정을 올바른 값으로 초기화해야 한다" {
        // given
        val dataInitializer = DataInitializer(pointConfigJpaRepository, pointEntityMapper)
        
        // when
        dataInitializer.run()
        
        // then
        val config = pointConfigJpaRepository.findByConfigKey("MAX_BALANCE_PER_MEMBER")
        config.isPresent shouldBe true
        val domainConfig = pointEntityMapper.toDomain(config.get())
        domainConfig.configValue shouldBe "10000000"
        domainConfig.description shouldBe "개인별 최대 보유 금액"
    }
    
    "DataInitializer는 DEFAULT_EXPIRATION_DAYS 설정을 올바른 값으로 초기화해야 한다" {
        // given
        val dataInitializer = DataInitializer(pointConfigJpaRepository, pointEntityMapper)
        
        // when
        dataInitializer.run()
        
        // then
        val config = pointConfigJpaRepository.findByConfigKey("DEFAULT_EXPIRATION_DAYS")
        config.isPresent shouldBe true
        val domainConfig = pointEntityMapper.toDomain(config.get())
        domainConfig.configValue shouldBe "365"
        domainConfig.description shouldBe "기본 만료일 (일)"
    }
    
    "DataInitializer는 MIN_EXPIRATION_DAYS 설정을 올바른 값으로 초기화해야 한다" {
        // given
        val dataInitializer = DataInitializer(pointConfigJpaRepository, pointEntityMapper)
        
        // when
        dataInitializer.run()
        
        // then
        val config = pointConfigJpaRepository.findByConfigKey("MIN_EXPIRATION_DAYS")
        config.isPresent shouldBe true
        val domainConfig = pointEntityMapper.toDomain(config.get())
        domainConfig.configValue shouldBe "1"
        domainConfig.description shouldBe "최소 만료일 (일)"
    }
    
    "DataInitializer는 MAX_EXPIRATION_DAYS 설정을 올바른 값으로 초기화해야 한다" {
        // given
        val dataInitializer = DataInitializer(pointConfigJpaRepository, pointEntityMapper)
        
        // when
        dataInitializer.run()
        
        // then
        val config = pointConfigJpaRepository.findByConfigKey("MAX_EXPIRATION_DAYS")
        config.isPresent shouldBe true
        val domainConfig = pointEntityMapper.toDomain(config.get())
        domainConfig.configValue shouldBe "1824"
        domainConfig.description shouldBe "최대 만료일 (일, 약 5년)"
    }
    
    "DataInitializer는 이미 존재하는 설정을 건너뛰고 새로 생성하지 않아야 한다" {
        // given
        val dataInitializer = DataInitializer(pointConfigJpaRepository, pointEntityMapper)
        
        // 초기 실행
        dataInitializer.run()
        
        // 이미 존재하는 설정 확인
        val firstRun = pointConfigJpaRepository.findByConfigKey("MAX_ACCUMULATION_AMOUNT_PER_TIME")
        firstRun.isPresent shouldBe true
        val firstRunEntity = firstRun.get()
        
        // when - 다시 실행
        dataInitializer.run()
        
        // then - 동일한 엔티티가 유지되어야 함 (새로 생성되지 않음)
        val allConfigs = pointConfigJpaRepository.findAll()
        allConfigs.size shouldBe 5  // 여전히 5개의 설정만 있어야 함
        
        val secondRun = pointConfigJpaRepository.findByConfigKey("MAX_ACCUMULATION_AMOUNT_PER_TIME")
        secondRun.isPresent shouldBe true
        secondRun.get().id shouldBe firstRunEntity.id  // 동일한 ID여야 함
    }
    
    "DataInitializer는 모든 설정이 올바르게 저장되었는지 확인할 수 있어야 한다" {
        // given
        val dataInitializer = DataInitializer(pointConfigJpaRepository, pointEntityMapper)
        
        // when
        dataInitializer.run()
        
        // then - 모든 설정의 개수 확인
        val allConfigs = pointConfigJpaRepository.findAll()
        allConfigs.size shouldBe 5
        
        // 모든 설정의 ID가 null이 아니어야 함 (저장되었음을 의미)
        allConfigs.forEach { config ->
            config.id shouldNotBe null
            config.configKey.isNotBlank() shouldBe true
            config.configValue.isNotBlank() shouldBe true
            config.createdAt shouldNotBe null
            config.updatedAt shouldNotBe null
        }
    }
    
    "DataInitializer는 저장된 설정을 도메인 엔티티로 변환할 수 있어야 한다" {
        // given
        val dataInitializer = DataInitializer(pointConfigJpaRepository, pointEntityMapper)
        
        // when
        dataInitializer.run()
        
        // then
        val configEntity = pointConfigJpaRepository.findByConfigKey("MAX_ACCUMULATION_AMOUNT_PER_TIME").get()
        val domainConfig = pointEntityMapper.toDomain(configEntity)
        
        domainConfig.configKey shouldBe "MAX_ACCUMULATION_AMOUNT_PER_TIME"
        domainConfig.configValue shouldBe "100000"
        domainConfig.description shouldBe "1회 최대 적립 금액"
        domainConfig.id shouldNotBe null
    }
})
