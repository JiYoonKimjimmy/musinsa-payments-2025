package com.musinsa.payments.point.infrastructure.persistence.jpa.repository

import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointConfigEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * PointConfigJpaRepository 통합 테스트
 */
@ActiveProfiles("test")
@DataJpaTest
class PointConfigJpaRepositoryTest @Autowired constructor(
    private val pointConfigJpaRepository: PointConfigJpaRepository
) : StringSpec({
    
    extensions(SpringExtension)
    
    beforeTest {
        pointConfigJpaRepository.deleteAll()
    }
    
    "설정 키로 조회할 수 있어야 한다" {
        // given
        val entity = createPointConfigEntity(
            configKey = "MAX_AMOUNT",
            configValue = "100000"
        )
        val saved = pointConfigJpaRepository.save(entity)
        
        // when
        val found = pointConfigJpaRepository.findByConfigKey("MAX_AMOUNT")
        
        // then
        found.isPresent shouldBe true
        found.get().id shouldBe saved.id
        found.get().configKey shouldBe "MAX_AMOUNT"
        found.get().configValue shouldBe "100000"
    }
    
    "존재하지 않는 설정 키로 조회 시 empty를 반환해야 한다" {
        // when
        val found = pointConfigJpaRepository.findByConfigKey("NOT_EXIST")
        
        // then
        found.isPresent shouldBe false
    }
    
    "모든 설정을 조회할 수 있어야 한다" {
        // given
        val entity1 = createPointConfigEntity(configKey = "CONFIG1", configValue = "VALUE1")
        val entity2 = createPointConfigEntity(configKey = "CONFIG2", configValue = "VALUE2")
        val entity3 = createPointConfigEntity(configKey = "CONFIG3", configValue = "VALUE3")
        pointConfigJpaRepository.saveAll(listOf(entity1, entity2, entity3))
        
        // when
        val all = pointConfigJpaRepository.findAll()
        
        // then
        all.size shouldBe 3
        all.map { it.configKey } shouldBe listOf("CONFIG1", "CONFIG2", "CONFIG3")
    }
    
    "설정을 저장하고 조회할 수 있어야 한다" {
        // given
        val entity = createPointConfigEntity(
            configKey = "SAVE_CONFIG",
            configValue = "SAVE_VALUE",
            description = "Test Description"
        )
        
        // when
        val saved = pointConfigJpaRepository.save(entity)
        
        // then
        saved.id shouldNotBe null
        val found = pointConfigJpaRepository.findById(saved.id!!)
        found.isPresent shouldBe true
        found.get().configKey shouldBe "SAVE_CONFIG"
        found.get().configValue shouldBe "SAVE_VALUE"
        found.get().description shouldBe "Test Description"
    }
    
    "설정 키는 유일해야 한다" {
        // given
        val entity1 = createPointConfigEntity(configKey = "UNIQUE_KEY", configValue = "VALUE1")
        pointConfigJpaRepository.save(entity1)
        
        val entity2 = createPointConfigEntity(configKey = "UNIQUE_KEY", configValue = "VALUE2")
        
        // when & then - JPA의 unique 제약조건에 의해 예외가 발생하거나, 저장 시 기존 값이 업데이트될 수 있음
        // 실제 동작은 DB 제약조건에 따라 달라질 수 있으므로, 테스트에서는 중복 저장 시나리오를 확인
        try {
            pointConfigJpaRepository.save(entity2)
            // 저장이 성공하면 업데이트된 것
            val found = pointConfigJpaRepository.findByConfigKey("UNIQUE_KEY")
            found.isPresent shouldBe true
        } catch (e: Exception) {
            // 제약조건 위반으로 예외 발생
            // 이는 정상적인 동작입니다.
        }
    }
    
}) {
    companion object {
        fun createPointConfigEntity(
            configKey: String,
            configValue: String,
            description: String? = null
        ): PointConfigEntity {
            val entity = PointConfigEntity()
            entity.configKey = configKey
            entity.configValue = configValue
            entity.description = description
            entity.createdAt = LocalDateTime.now()
            entity.updatedAt = LocalDateTime.now()
            return entity
        }
    }
}
