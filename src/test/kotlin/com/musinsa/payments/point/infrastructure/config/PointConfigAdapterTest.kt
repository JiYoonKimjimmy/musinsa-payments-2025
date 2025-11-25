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
import java.time.LocalDateTime

/**
 * PointConfigAdapter 통합 테스트
 * Adapter의 메서드와 도메인-JPA 엔티티 변환을 함께 검증합니다.
 */
@Import(PointEntityMapper::class)
@ActiveProfiles("test")
@DataJpaTest
class PointConfigAdapterTest @Autowired constructor(
    private val pointConfigJpaRepository: PointConfigJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : StringSpec({
    
    extensions(SpringExtension)
    
    lateinit var adapter: PointConfigAdapter
    
    beforeTest {
        pointConfigJpaRepository.deleteAll()
        adapter = PointConfigAdapter(
            pointConfigJpaRepository,
            pointEntityMapper
        )
    }
    
    "도메인 엔티티를 저장하고 조회할 수 있어야 한다" {
        // given
        val config = createPointConfig(
            configKey = "TEST_KEY",
            configValue = "100000",
            description = "테스트 설정"
        )
        
        // when
        val saved = adapter.save(config)
        
        // then
        saved.id shouldNotBe null
        saved.configKey shouldBe "TEST_KEY"
        saved.configValue shouldBe "100000"
        saved.description shouldBe "테스트 설정"
    }
    
    "설정 키로 도메인 엔티티를 조회할 수 있어야 한다" {
        // given
        val config = createPointConfig(
            configKey = "MAX_AMOUNT",
            configValue = "100000"
        )
        val saved = adapter.save(config)
        
        // when
        val found = adapter.findByConfigKey("MAX_AMOUNT")
        
        // then
        found.isPresent shouldBe true
        found.get().id shouldBe saved.id
        found.get().configKey shouldBe "MAX_AMOUNT"
        found.get().configValue shouldBe "100000"
    }
    
    "존재하지 않는 설정 키로 조회 시 empty를 반환해야 한다" {
        // when
        val found = adapter.findByConfigKey("NOT_EXIST")
        
        // then
        found.isPresent shouldBe false
    }
    
    "모든 도메인 엔티티를 조회할 수 있어야 한다" {
        // given
        val config1 = createPointConfig(configKey = "CONFIG1", configValue = "VALUE1")
        val config2 = createPointConfig(configKey = "CONFIG2", configValue = "VALUE2")
        val config3 = createPointConfig(configKey = "CONFIG3", configValue = "VALUE3")
        adapter.save(config1)
        adapter.save(config2)
        adapter.save(config3)
        
        // when
        val all = adapter.findAll()
        
        // then
        all.size shouldBe 3
        all.map { it.configKey }.toSet() shouldBe setOf("CONFIG1", "CONFIG2", "CONFIG3")
    }
    
    "저장 시 ID가 자동으로 생성되어야 한다" {
        // given
        val config = createPointConfig(
            configKey = "SAVE_CONFIG",
            configValue = "SAVE_VALUE"
        )
        config.id shouldBe null  // 저장 전에는 null
        
        // when
        val saved = adapter.save(config)
        
        // then
        saved.id shouldNotBe null
    }
    
    "도메인 엔티티의 모든 필드가 올바르게 저장되고 조회되어야 한다" {
        // given
        val config = PointConfig(
            configKey = "FULL_CONFIG",
            configValue = "50000",
            description = "전체 필드 테스트",
            createdAt = LocalDateTime.now().minusHours(1),
            updatedAt = LocalDateTime.now().minusHours(1)
        )
        
        // when
        val saved = adapter.save(config)
        val found = adapter.findByConfigKey("FULL_CONFIG")
        
        // then
        found.isPresent shouldBe true
        val retrieved = found.get()
        retrieved.id shouldBe saved.id
        retrieved.configKey shouldBe "FULL_CONFIG"
        retrieved.configValue shouldBe "50000"
        retrieved.description shouldBe "전체 필드 테스트"
    }
    
    "설정 값을 타입 변환하여 조회할 수 있어야 한다" {
        // given
        val config = createPointConfig(
            configKey = "LONG_VALUE",
            configValue = "100000"
        )
        val saved = adapter.save(config)
        
        // when
        val found = adapter.findByConfigKey("LONG_VALUE")
        
        // then
        found.isPresent shouldBe true
        val retrieved = found.get()
        retrieved.getLongValue() shouldBe 100000L
        retrieved.getIntValue() shouldBe 100000
    }
    
}) {
    companion object {
        fun createPointConfig(
            configKey: String,
            configValue: String,
            description: String? = null
        ): PointConfig {
            return PointConfig(
                configKey = configKey,
                configValue = configValue,
                description = description
            )
        }
    }
}
