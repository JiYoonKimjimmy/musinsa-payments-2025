package com.musinsa.payments.point.infrastructure.config

import com.musinsa.payments.point.domain.entity.PointConfigHistory
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointConfigHistoryJpaRepository
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
 * PointConfigHistoryAdapter 통합 테스트
 * Adapter의 메서드와 도메인-JPA 엔티티 변환을 함께 검증합니다.
 */
@Import(PointEntityMapper::class)
@ActiveProfiles("test")
@DataJpaTest
class PointConfigHistoryAdapterTest @Autowired constructor(
    private val pointConfigHistoryJpaRepository: PointConfigHistoryJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : StringSpec({
    
    extensions(SpringExtension)
    
    lateinit var adapter: PointConfigHistoryAdapter
    
    beforeTest {
        pointConfigHistoryJpaRepository.deleteAll()
        adapter = PointConfigHistoryAdapter(
            pointConfigHistoryJpaRepository,
            pointEntityMapper
        )
    }
    
    "도메인 엔티티를 저장하고 조회할 수 있어야 한다" {
        // given
        val history = PointConfigHistory(
            configKey = "MAX_ACCUMULATION_AMOUNT_PER_TIME",
            newValue = "200000",
            oldValue = "100000",
            changedBy = "admin",
            changedAt = LocalDateTime.now()
        )
        
        // when
        val saved = adapter.save(history)
        
        // then
        saved.id shouldNotBe null
        saved.configKey shouldBe "MAX_ACCUMULATION_AMOUNT_PER_TIME"
        saved.newValue shouldBe "200000"
        saved.oldValue shouldBe "100000"
        saved.changedBy shouldBe "admin"
    }
    
    "설정 키로 변경 이력을 조회할 수 있어야 한다" {
        // given
        val history1 = PointConfigHistory(
            configKey = "MAX_ACCUMULATION_AMOUNT_PER_TIME",
            newValue = "200000",
            oldValue = "100000",
            changedBy = "admin",
            changedAt = LocalDateTime.now().minusDays(1)
        )
        val history2 = PointConfigHistory(
            configKey = "MAX_ACCUMULATION_AMOUNT_PER_TIME",
            newValue = "300000",
            oldValue = "200000",
            changedBy = "admin",
            changedAt = LocalDateTime.now()
        )
        val otherHistory = PointConfigHistory(
            configKey = "MAX_BALANCE_PER_MEMBER",
            newValue = "20000000",
            oldValue = "10000000",
            changedBy = "admin",
            changedAt = LocalDateTime.now()
        )
        
        adapter.save(history1)
        adapter.save(history2)
        adapter.save(otherHistory)
        
        // when
        val histories = adapter.findByConfigKey("MAX_ACCUMULATION_AMOUNT_PER_TIME")
        
        // then
        histories.size shouldBe 2
        histories[0].newValue shouldBe "300000"  // 최신순
        histories[1].newValue shouldBe "200000"
    }
    
    "설정 키가 다른 이력은 조회되지 않아야 한다" {
        // given
        val history1 = PointConfigHistory(
            configKey = "MAX_ACCUMULATION_AMOUNT_PER_TIME",
            newValue = "200000",
            oldValue = "100000"
        )
        val history2 = PointConfigHistory(
            configKey = "MAX_BALANCE_PER_MEMBER",
            newValue = "20000000",
            oldValue = "10000000"
        )
        
        adapter.save(history1)
        adapter.save(history2)
        
        // when
        val histories = adapter.findByConfigKey("MAX_ACCUMULATION_AMOUNT_PER_TIME")
        
        // then
        histories.size shouldBe 1
        histories[0].configKey shouldBe "MAX_ACCUMULATION_AMOUNT_PER_TIME"
    }
    
    "변경 이력이 없으면 빈 목록을 반환해야 한다" {
        // when
        val histories = adapter.findByConfigKey("NOT_FOUND")
        
        // then
        histories.isEmpty() shouldBe true
    }
})

