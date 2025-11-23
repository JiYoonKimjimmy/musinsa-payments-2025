package com.musinsa.payments.point.infrastructure.persistence.adapter

import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.entity.PointUsageDetail
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import com.musinsa.payments.point.domain.valueobject.PointKey
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointAccumulationJpaRepository
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointUsageDetailJpaRepository
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointUsageJpaRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * PointUsageDetailPersistenceAdapter 통합 테스트
 * Adapter의 메서드와 도메인-JPA 엔티티 변환을 함께 검증합니다.
 */
@Import(PointEntityMapper::class)
@ActiveProfiles("test")
@DataJpaTest
class PointUsageDetailPersistenceAdapterTest @Autowired constructor(
    private val pointUsageDetailJpaRepository: PointUsageDetailJpaRepository,
    private val pointUsageJpaRepository: PointUsageJpaRepository,
    private val pointAccumulationJpaRepository: PointAccumulationJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : StringSpec({
    
    extensions(SpringExtension)
    
    lateinit var adapter: PointUsageDetailPersistenceAdapter

    beforeTest {
        pointUsageDetailJpaRepository.deleteAll()
        pointUsageJpaRepository.deleteAll()
        pointAccumulationJpaRepository.deleteAll()

        adapter = PointUsageDetailPersistenceAdapter(pointUsageDetailJpaRepository, pointEntityMapper)
    }
    
    // Helper functions
    fun createAndSaveUsage(
        pointKey: PointKey,
        memberId: Long,
        orderNumber: OrderNumber = OrderNumber.of("ORDER123")
    ): PointUsage {
        val usage = PointUsage(
            pointKey = pointKey.value,
            memberId = memberId,
            orderNumber = orderNumber,
            totalAmount = Money.of(10000L)
        )
        val entity = pointEntityMapper.toEntity(usage)
        val savedEntity = pointUsageJpaRepository.save(entity)
        return pointEntityMapper.toDomain(savedEntity)
    }
    
    fun createAndSaveAccumulation(
        pointKey: PointKey,
        memberId: Long,
        amount: Money = Money.of(10000L)
    ): PointAccumulation {
        val accumulation = PointAccumulation(
            pointKey = pointKey.value,
            memberId = memberId,
            amount = amount,
            expirationDate = LocalDate.now().plusDays(365)
        )
        val entity = pointEntityMapper.toEntity(accumulation)
        val savedEntity = pointAccumulationJpaRepository.save(entity)
        return pointEntityMapper.toDomain(savedEntity)
    }
    
    fun createPointUsageDetail(
        pointUsageId: Long,
        pointAccumulationId: Long,
        amount: Money,
        cancelledAmount: Money = Money.ZERO
    ): PointUsageDetail {
        return PointUsageDetail(
            pointUsageId = pointUsageId,
            pointAccumulationId = pointAccumulationId,
            amount = amount,
            cancelledAmount = cancelledAmount
        )
    }
    
    "도메인 엔티티 목록을 일괄 저장하고 조회할 수 있어야 한다" {
        // given
        val usage = createAndSaveUsage(pointKey = PointKey.of("USAGE1"), memberId = 1L)
        val accumulation = createAndSaveAccumulation(pointKey = PointKey.of("ACC1"), memberId = 1L)
        
        val detail1 = createPointUsageDetail(
            pointUsageId = usage.id!!,
            pointAccumulationId = accumulation.id!!,
            amount = Money.of(1000L)
        )
        val detail2 = createPointUsageDetail(
            pointUsageId = usage.id!!,
            pointAccumulationId = accumulation.id!!,
            amount = Money.of(2000L)
        )
        
        // when
        val saved = adapter.saveAll(listOf(detail1, detail2))
        
        // then
        saved.size shouldBe 2
        saved.all { it.id != null } shouldBe true
        saved[0].amount shouldBe Money.of(1000L)
        saved[1].amount shouldBe Money.of(2000L)
    }
    
    "포인트 사용 키로 도메인 엔티티 목록을 조회할 수 있어야 한다" {
        // given
        val usageKey = PointKey.of("USAGE123")
        val usage = createAndSaveUsage(pointKey = usageKey, memberId = 1L)
        val accumulation = createAndSaveAccumulation(pointKey = PointKey.of("ACC1"), memberId = 1L)
        
        val detail1 = createPointUsageDetail(
            pointUsageId = usage.id!!,
            pointAccumulationId = accumulation.id!!,
            amount = Money.of(1000L)
        )
        val detail2 = createPointUsageDetail(
            pointUsageId = usage.id!!,
            pointAccumulationId = accumulation.id!!,
            amount = Money.of(2000L)
        )
        adapter.saveAll(listOf(detail1, detail2))
        
        // when
        val found = adapter.findByUsagePointKey("USAGE123")
        
        // then
        found.size shouldBe 2
        found.all { it.pointUsageId == usage.id } shouldBe true
    }
    
    "포인트 적립 키로 도메인 엔티티 목록을 조회할 수 있어야 한다" {
        // given
        val accumulationKey = PointKey.of("ACC123")
        val usage = createAndSaveUsage(pointKey = PointKey.of("USAGE1"), memberId = 1L)
        val accumulation = createAndSaveAccumulation(pointKey = accumulationKey, memberId = 1L)
        
        val detail1 = createPointUsageDetail(
            pointUsageId = usage.id!!,
            pointAccumulationId = accumulation.id!!,
            amount = Money.of(1000L)
        )
        val detail2 = createPointUsageDetail(
            pointUsageId = usage.id!!,
            pointAccumulationId = accumulation.id!!,
            amount = Money.of(2000L)
        )
        adapter.saveAll(listOf(detail1, detail2))
        
        // when
        val found = adapter.findByAccumulationPointKey("ACC123")
        
        // then
        found.size shouldBe 2
        found.all { it.pointAccumulationId == accumulation.id } shouldBe true
    }
    
    "여러 적립 건에서 사용된 상세 내역을 조회할 수 있어야 한다" {
        // given
        val usage = createAndSaveUsage(pointKey = PointKey.of("USAGE1"), memberId = 1L)
        val accumulation1 = createAndSaveAccumulation(pointKey = PointKey.of("ACC1"), memberId = 1L)
        val accumulation2 = createAndSaveAccumulation(pointKey = PointKey.of("ACC2"), memberId = 1L)
        
        val detail1 = createPointUsageDetail(
            pointUsageId = usage.id!!,
            pointAccumulationId = accumulation1.id!!,
            amount = Money.of(1000L)
        )
        val detail2 = createPointUsageDetail(
            pointUsageId = usage.id!!,
            pointAccumulationId = accumulation2.id!!,
            amount = Money.of(2000L)
        )
        adapter.saveAll(listOf(detail1, detail2))
        
        // when
        val found = adapter.findByUsagePointKey("USAGE1")
        
        // then
        found.size shouldBe 2
        found.map { it.pointAccumulationId }.toSet() shouldBe setOf(accumulation1.id, accumulation2.id)
    }
    
    "저장 시 ID가 자동으로 생성되어야 한다" {
        // given
        val usage = createAndSaveUsage(pointKey = PointKey.generate(), memberId = 1L)
        val accumulation = createAndSaveAccumulation(pointKey = PointKey.generate(), memberId = 1L)
        
        val detail = createPointUsageDetail(
            pointUsageId = usage.id!!,
            pointAccumulationId = accumulation.id!!,
            amount = Money.of(1000L)
        )
        detail.id shouldBe null  // 저장 전에는 null
        
        // when
        val saved = adapter.saveAll(listOf(detail))
        
        // then
        saved[0].id shouldNotBe null
    }
    
    "도메인 엔티티의 모든 필드가 올바르게 저장되고 조회되어야 한다" {
        // given
        val usageKey = PointKey.generate()
        val usage = createAndSaveUsage(pointKey = usageKey, memberId = 1L)
        val accumulation = createAndSaveAccumulation(pointKey = PointKey.generate(), memberId = 1L)
        
        val detail = PointUsageDetail(
            pointUsageId = usage.id!!,
            pointAccumulationId = accumulation.id!!,
            amount = Money.of(5000L),
            cancelledAmount = Money.of(2000L),
            createdAt = LocalDateTime.now().minusHours(1),
            updatedAt = LocalDateTime.now().minusHours(1)
        )
        
        // when
        adapter.saveAll(listOf(detail))

        // then
        val found = adapter.findByUsagePointKey(usageKey.value)
        found.isNotEmpty() shouldBe true

        val retrieved = found.first { it.pointAccumulationId == accumulation.id }
        retrieved.id shouldNotBe null
        retrieved.pointUsageId shouldBe usage.id
        retrieved.pointAccumulationId shouldBe accumulation.id
        retrieved.amount shouldBe Money.of(5000L)
        retrieved.cancelledAmount shouldBe Money.of(2000L)
    }
    
})
