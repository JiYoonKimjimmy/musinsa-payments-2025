package com.musinsa.payments.point.infrastructure.persistence.jpa.repository

import com.musinsa.payments.point.domain.entity.PointUsageStatus
import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointUsageEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * PointUsageJpaRepository 통합 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
class PointUsageJpaRepositoryTest @Autowired constructor(
    private val pointUsageJpaRepository: PointUsageJpaRepository
) : StringSpec({
    
    extensions(SpringExtension)
    
    beforeTest {
        pointUsageJpaRepository.deleteAll()
    }
    
    "포인트 키로 조회할 수 있어야 한다" {
        // given
        val entity = createPointUsageEntity(
            pointKey = "USAGE123",
            memberId = 1L,
            orderNumber = "ORDER123"
        )
        val saved = pointUsageJpaRepository.save(entity)
        
        // when
        val found = pointUsageJpaRepository.findByPointKey("USAGE123")
        
        // then
        found.isPresent shouldBe true
        found.get().id shouldBe saved.id
        found.get().pointKey shouldBe "USAGE123"
    }
    
    "회원 ID와 주문번호로 조회할 수 있어야 한다" {
        // given
        val entity1 = createPointUsageEntity(
            pointKey = "USAGE1",
            memberId = 1L,
            orderNumber = "ORDER123"
        )
        val entity2 = createPointUsageEntity(
            pointKey = "USAGE2",
            memberId = 1L,
            orderNumber = "ORDER123"
        )
        val entity3 = createPointUsageEntity(
            pointKey = "USAGE3",
            memberId = 1L,
            orderNumber = "ORDER456"
        )
        pointUsageJpaRepository.saveAll(listOf(entity1, entity2, entity3))
        
        // when
        val found = pointUsageJpaRepository.findByMemberIdAndOrderNumber(1L, "ORDER123")
        
        // then
        found.size shouldBe 2
        found.all { it.memberId == 1L } shouldBe true
        found.all { it.orderNumber == "ORDER123" } shouldBe true
    }
    
    "회원 ID로 사용 내역을 페이징하여 조회할 수 있어야 한다" {
        // given
        val baseTime = LocalDateTime.now()
        val entity1 = createPointUsageEntity(
            pointKey = "USAGE1", 
            memberId = 1L, 
            orderNumber = "ORDER1",
            createdAt = baseTime.minusSeconds(2)
        )
        val entity2 = createPointUsageEntity(
            pointKey = "USAGE2", 
            memberId = 1L, 
            orderNumber = "ORDER2",
            createdAt = baseTime.minusSeconds(1)
        )
        val entity3 = createPointUsageEntity(
            pointKey = "USAGE3", 
            memberId = 1L, 
            orderNumber = "ORDER3",
            createdAt = baseTime
        )
        val entity4 = createPointUsageEntity(
            pointKey = "USAGE4", 
            memberId = 2L, 
            orderNumber = "ORDER4",
            createdAt = baseTime
        )
        pointUsageJpaRepository.saveAll(listOf(entity1, entity2, entity3, entity4))
        
        val pageable = PageRequest.of(0, 2)
        
        // when
        val page = pointUsageJpaRepository.findByMemberId(1L, null, pageable)
        
        // then
        page.totalElements shouldBe 3
        page.content.size shouldBe 2
        page.content.all { it.memberId == 1L } shouldBe true
        page.content[0].pointKey shouldBe "USAGE3"  // 최신순
        page.content[1].pointKey shouldBe "USAGE2"
    }
    
    "주문번호로 필터링하여 조회할 수 있어야 한다" {
        // given
        val entity1 = createPointUsageEntity(pointKey = "USAGE1", memberId = 1L, orderNumber = "ORDER123")
        val entity2 = createPointUsageEntity(pointKey = "USAGE2", memberId = 1L, orderNumber = "ORDER123")
        val entity3 = createPointUsageEntity(pointKey = "USAGE3", memberId = 1L, orderNumber = "ORDER456")
        pointUsageJpaRepository.saveAll(listOf(entity1, entity2, entity3))
        
        val pageable = PageRequest.of(0, 10)
        
        // when
        val page = pointUsageJpaRepository.findByMemberId(1L, "ORDER123", pageable)
        
        // then
        page.totalElements shouldBe 2
        page.content.all { it.orderNumber == "ORDER123" } shouldBe true
    }
    
    "사용 건을 저장하고 조회할 수 있어야 한다" {
        // given
        val entity = createPointUsageEntity(
            pointKey = "SAVE123",
            memberId = 1L,
            orderNumber = "ORDER123"
        )
        
        // when
        val saved = pointUsageJpaRepository.save(entity)
        
        // then
        saved.id shouldNotBe null
        val found = pointUsageJpaRepository.findById(saved.id!!)
        found.isPresent shouldBe true
        found.get().pointKey shouldBe "SAVE123"
    }
    
}) {
    companion object {
        fun createPointUsageEntity(
            pointKey: String,
            memberId: Long,
            orderNumber: String,
            totalAmount: BigDecimal = BigDecimal("1000"),
            cancelledAmount: BigDecimal = BigDecimal.ZERO,
            status: PointUsageStatus = PointUsageStatus.USED,
            createdAt: LocalDateTime = LocalDateTime.now()
        ): PointUsageEntity {
            val entity = PointUsageEntity()
            entity.pointKey = pointKey
            entity.memberId = memberId
            entity.orderNumber = orderNumber
            entity.totalAmount = totalAmount
            entity.cancelledAmount = cancelledAmount
            entity.status = status
            entity.createdAt = createdAt
            entity.updatedAt = LocalDateTime.now()
            return entity
        }
    }
}
