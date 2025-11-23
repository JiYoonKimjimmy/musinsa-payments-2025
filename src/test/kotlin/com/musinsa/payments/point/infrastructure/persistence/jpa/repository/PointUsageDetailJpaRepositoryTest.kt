package com.musinsa.payments.point.infrastructure.persistence.jpa.repository

import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.domain.entity.PointUsageStatus
import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointAccumulationEntity
import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointUsageDetailEntity
import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointUsageEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * PointUsageDetailJpaRepository 통합 테스트
 */
@ActiveProfiles("test")
@DataJpaTest
class PointUsageDetailJpaRepositoryTest @Autowired constructor(
    private val pointUsageDetailJpaRepository: PointUsageDetailJpaRepository,
    private val pointUsageJpaRepository: PointUsageJpaRepository,
    private val pointAccumulationJpaRepository: PointAccumulationJpaRepository
) : StringSpec({
    
    extensions(SpringExtension)
    
    beforeTest {
        pointUsageDetailJpaRepository.deleteAll()
        pointUsageJpaRepository.deleteAll()
        pointAccumulationJpaRepository.deleteAll()
    }
    
    "포인트 사용 ID로 조회할 수 있어야 한다" {
        // given
        val usageEntity = createPointUsageEntity(pointKey = "USAGE1")
        val savedUsage = pointUsageJpaRepository.save(usageEntity)
        
        val detail1 = createPointUsageDetailEntity(
            pointUsageId = savedUsage.id!!,
            pointAccumulationId = 1L
        )
        val detail2 = createPointUsageDetailEntity(
            pointUsageId = savedUsage.id!!,
            pointAccumulationId = 2L
        )
        pointUsageDetailJpaRepository.saveAll(listOf(detail1, detail2))
        
        // when
        val found = pointUsageDetailJpaRepository.findByPointUsageId(savedUsage.id!!)
        
        // then
        found.size shouldBe 2
        found.all { it.pointUsageId == savedUsage.id } shouldBe true
    }
    
    "포인트 적립 ID로 조회할 수 있어야 한다" {
        // given
        val accumulationEntity = createPointAccumulationEntity(pointKey = "ACC1")
        val savedAccumulation = pointAccumulationJpaRepository.save(accumulationEntity)
        
        val detail1 = createPointUsageDetailEntity(
            pointUsageId = 1L,
            pointAccumulationId = savedAccumulation.id!!
        )
        val detail2 = createPointUsageDetailEntity(
            pointUsageId = 2L,
            pointAccumulationId = savedAccumulation.id!!
        )
        pointUsageDetailJpaRepository.saveAll(listOf(detail1, detail2))
        
        // when
        val found = pointUsageDetailJpaRepository.findByPointAccumulationId(savedAccumulation.id!!)
        
        // then
        found.size shouldBe 2
        found.all { it.pointAccumulationId == savedAccumulation.id } shouldBe true
    }
    
    "포인트 사용 키로 조회할 수 있어야 한다" {
        // given
        val usageEntity = createPointUsageEntity(pointKey = "USAGE123")
        val savedUsage = pointUsageJpaRepository.save(usageEntity)
        
        val detail = createPointUsageDetailEntity(
            pointUsageId = savedUsage.id!!,
            pointAccumulationId = 1L
        )
        pointUsageDetailJpaRepository.save(detail)
        
        // when
        val found = pointUsageDetailJpaRepository.findByUsagePointKey("USAGE123")
        
        // then
        found.size shouldBe 1
        found[0].pointUsageId shouldBe savedUsage.id
    }
    
    "포인트 적립 키로 조회할 수 있어야 한다" {
        // given
        val accumulationEntity = createPointAccumulationEntity(pointKey = "ACC123")
        val savedAccumulation = pointAccumulationJpaRepository.save(accumulationEntity)
        
        val detail = createPointUsageDetailEntity(
            pointUsageId = 1L,
            pointAccumulationId = savedAccumulation.id!!
        )
        pointUsageDetailJpaRepository.save(detail)
        
        // when
        val found = pointUsageDetailJpaRepository.findByAccumulationPointKey("ACC123")
        
        // then
        found.size shouldBe 1
        found[0].pointAccumulationId shouldBe savedAccumulation.id
    }
    
    "사용 상세 내역을 저장하고 조회할 수 있어야 한다" {
        // given
        val usageEntity = createPointUsageEntity(pointKey = "USAGE1")
        val savedUsage = pointUsageJpaRepository.save(usageEntity)
        
        val detail = createPointUsageDetailEntity(
            pointUsageId = savedUsage.id!!,
            pointAccumulationId = 1L,
            amount = BigDecimal("500")
        )
        
        // when
        val saved = pointUsageDetailJpaRepository.save(detail)
        
        // then
        saved.id shouldNotBe null
        val found = pointUsageDetailJpaRepository.findById(saved.id!!)
        found.isPresent shouldBe true
        found.get().amount shouldBe BigDecimal("500")
    }
    
}) {
    companion object {
        fun createPointUsageEntity(
            pointKey: String,
            memberId: Long = 1L,
            orderNumber: String = "ORDER123"
        ): PointUsageEntity {
            val entity = PointUsageEntity()
            entity.pointKey = pointKey
            entity.memberId = memberId
            entity.orderNumber = orderNumber
            entity.totalAmount = BigDecimal("1000")
            entity.cancelledAmount = BigDecimal.ZERO
            entity.status = PointUsageStatus.USED
            entity.createdAt = LocalDateTime.now()
            entity.updatedAt = LocalDateTime.now()
            return entity
        }
        
        fun createPointAccumulationEntity(
            pointKey: String,
            memberId: Long = 1L
        ): PointAccumulationEntity {
            val entity = PointAccumulationEntity()
            entity.pointKey = pointKey
            entity.memberId = memberId
            entity.amount = BigDecimal("1000")
            entity.availableAmount = BigDecimal("1000")
            entity.expirationDate = LocalDate.now().plusDays(365)
            entity.isManualGrant = false
            entity.status = PointAccumulationStatus.ACCUMULATED
            entity.createdAt = LocalDateTime.now()
            entity.updatedAt = LocalDateTime.now()
            return entity
        }
        
        fun createPointUsageDetailEntity(
            pointUsageId: Long,
            pointAccumulationId: Long,
            amount: BigDecimal = BigDecimal("1000"),
            cancelledAmount: BigDecimal = BigDecimal.ZERO
        ): PointUsageDetailEntity {
            val entity = PointUsageDetailEntity()
            entity.pointUsageId = pointUsageId
            entity.pointAccumulationId = pointAccumulationId
            entity.amount = amount
            entity.cancelledAmount = cancelledAmount
            entity.createdAt = LocalDateTime.now()
            entity.updatedAt = LocalDateTime.now()
            return entity
        }
    }
}
