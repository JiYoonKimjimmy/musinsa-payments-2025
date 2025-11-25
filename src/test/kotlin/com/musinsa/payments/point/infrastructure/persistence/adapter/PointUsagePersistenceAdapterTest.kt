package com.musinsa.payments.point.infrastructure.persistence.adapter

import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.entity.PointUsageStatus
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import com.musinsa.payments.point.domain.valueobject.PointKey
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointUsageJpaRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * PointUsagePersistenceAdapter 통합 테스트
 * Adapter의 메서드와 도메인-JPA 엔티티 변환을 함께 검증합니다.
 */
@Import(PointEntityMapper::class)
@ActiveProfiles("test")
@DataJpaTest
class PointUsagePersistenceAdapterTest @Autowired constructor(
    private val pointUsageJpaRepository: PointUsageJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : StringSpec({
    
    extensions(SpringExtension)
    
    lateinit var adapter: PointUsagePersistenceAdapter
    
    beforeTest {
        pointUsageJpaRepository.deleteAll()

        adapter = PointUsagePersistenceAdapter(pointUsageJpaRepository, pointEntityMapper)
    }
    
    "도메인 엔티티를 저장하고 조회할 수 있어야 한다" {
        // given
        val usage = createPointUsage(
            pointKey = PointKey.generate(),
            memberId = 1L,
            orderNumber = OrderNumber.of("ORDER123"),
            totalAmount = Money.of(5000L)
        )
        
        // when
        val saved = adapter.save(usage)
        
        // then
        saved.id shouldNotBe null
        saved.pointKey shouldBe usage.pointKey
        saved.memberId shouldBe 1L
        saved.orderNumber shouldBe usage.orderNumber
        saved.totalAmount shouldBe Money.of(5000L)
        saved.cancelledAmount shouldBe Money.ZERO
        saved.status shouldBe PointUsageStatus.USED
    }
    
    "포인트 키로 도메인 엔티티를 조회할 수 있어야 한다" {
        // given
        val pointKey = PointKey.of("USAGE1234")
        val usage = createPointUsage(
            pointKey = pointKey,
            memberId = 1L,
            orderNumber = OrderNumber.of("ORDER123")
        )
        val saved = adapter.save(usage)
        
        // when
        val found = adapter.findByPointKey("USAGE1234")
        
        // then
        found.isPresent shouldBe true
        found.get().id shouldBe saved.id
        found.get().pointKey shouldBe "USAGE1234"
        found.get().memberId shouldBe 1L
    }
    
    "존재하지 않는 포인트 키로 조회 시 empty를 반환해야 한다" {
        // when
        val found = adapter.findByPointKey("NOTEXIST")
        
        // then
        found.isPresent shouldBe false
    }
    
    "회원 ID와 주문번호로 도메인 엔티티 목록을 조회할 수 있어야 한다" {
        // given
        val orderNumber = OrderNumber.of("ORDER123")
        val usage1 = createPointUsage(
            pointKey = PointKey.generate(),
            memberId = 1L,
            orderNumber = orderNumber
        )
        val usage2 = createPointUsage(
            pointKey = PointKey.generate(),
            memberId = 1L,
            orderNumber = orderNumber
        )
        val usage3 = createPointUsage(
            pointKey = PointKey.generate(),
            memberId = 1L,
            orderNumber = OrderNumber.of("ORDER456")
        )
        adapter.save(usage1)
        adapter.save(usage2)
        adapter.save(usage3)
        
        // when
        val found = adapter.findByMemberIdAndOrderNumber(1L, orderNumber)
        
        // then
        found.size shouldBe 2
        found.all { it.memberId == 1L } shouldBe true
        found.all { it.orderNumber == orderNumber } shouldBe true
    }
    
    "회원 ID로 사용 내역을 페이징하여 조회할 수 있어야 한다" {
        // given
        val usage1 = createPointUsage(pointKey = PointKey.generate(), memberId = 1L, orderNumber = OrderNumber.of("ORDER1"))
        val usage2 = createPointUsage(pointKey = PointKey.generate(), memberId = 1L, orderNumber = OrderNumber.of("ORDER2"))
        val usage3 = createPointUsage(pointKey = PointKey.generate(), memberId = 1L, orderNumber = OrderNumber.of("ORDER3"))
        val usage4 = createPointUsage(pointKey = PointKey.generate(), memberId = 2L, orderNumber = OrderNumber.of("ORDER4"))
        adapter.save(usage1)
        adapter.save(usage2)
        adapter.save(usage3)
        adapter.save(usage4)
        
        val pageable = PageRequest.of(0, 2)
        
        // when
        val page = adapter.findUsageHistoryByMemberId(1L, null, pageable)
        
        // then
        page.totalElements shouldBe 3
        page.content.size shouldBe 2
        page.content.all { it.memberId == 1L } shouldBe true
    }
    
    "주문번호로 필터링하여 조회할 수 있어야 한다" {
        // given
        val orderNumber = OrderNumber.of("ORDER123")
        val usage1 = createPointUsage(pointKey = PointKey.generate(), memberId = 1L, orderNumber = orderNumber)
        val usage2 = createPointUsage(pointKey = PointKey.generate(), memberId = 1L, orderNumber = orderNumber)
        val usage3 = createPointUsage(pointKey = PointKey.generate(), memberId = 1L, orderNumber = OrderNumber.of("ORDER456"))
        adapter.save(usage1)
        adapter.save(usage2)
        adapter.save(usage3)
        
        val pageable = PageRequest.of(0, 10)
        
        // when
        val page = adapter.findUsageHistoryByMemberId(1L, "ORDER123", pageable)
        
        // then
        page.totalElements shouldBe 2
        page.content.all { it.orderNumber.value == "ORDER123" } shouldBe true
    }
    
    "저장 시 ID가 자동으로 생성되어야 한다" {
        // given
        val usage = createPointUsage(
            pointKey = PointKey.generate(),
            memberId = 1L,
            orderNumber = OrderNumber.of("ORDER123")
        )
        usage.id shouldBe null  // 저장 전에는 null
        
        // when
        val saved = adapter.save(usage)
        
        // then
        saved.id shouldNotBe null
    }
    
    "도메인 엔티티의 모든 필드가 올바르게 저장되고 조회되어야 한다" {
        // given
        val pointKey = PointKey.generate()
        val orderNumber = OrderNumber.of("ORDER123")
        val usage = PointUsage(
            pointKey = pointKey.value,
            memberId = 1L,
            orderNumber = orderNumber,
            totalAmount = Money.of(3000L),
            cancelledAmount = Money.of(1000L),
            status = PointUsageStatus.PARTIALLY_CANCELLED,
            createdAt = LocalDateTime.now().minusHours(1),
            updatedAt = LocalDateTime.now().minusHours(1)
        )
        
        // when
        val saved = adapter.save(usage)
        val found = adapter.findByPointKey(pointKey.value)
        
        // then
        found.isPresent shouldBe true
        val retrieved = found.get()
        retrieved.id shouldBe saved.id
        retrieved.pointKey shouldBe pointKey.value
        retrieved.memberId shouldBe 1L
        retrieved.orderNumber shouldBe orderNumber
        retrieved.totalAmount shouldBe Money.of(3000L)
        retrieved.cancelledAmount shouldBe Money.of(1000L)
        retrieved.status shouldBe PointUsageStatus.PARTIALLY_CANCELLED
    }
    
}) {
    companion object {
        fun createPointUsage(
            pointKey: PointKey,
            memberId: Long,
            orderNumber: OrderNumber,
            totalAmount: Money = Money.of(1000L),
            cancelledAmount: Money = Money.ZERO,
            status: PointUsageStatus = PointUsageStatus.USED
        ): PointUsage {
            return PointUsage(
                pointKey = pointKey.value,
                memberId = memberId,
                orderNumber = orderNumber,
                totalAmount = totalAmount,
                cancelledAmount = cancelledAmount,
                status = status
            )
        }
    }
}
