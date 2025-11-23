package com.musinsa.payments.point.infrastructure.persistence.jpa.repository

import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.infrastructure.persistence.jpa.entity.PointAccumulationEntity
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
 * PointAccumulationJpaRepository 통합 테스트
 */
@ActiveProfiles("test")
@DataJpaTest
class PointAccumulationJpaRepositoryTest @Autowired constructor(
    val pointAccumulationJpaRepository: PointAccumulationJpaRepository
) : StringSpec({
    
    extensions(SpringExtension)
    
    beforeTest {
        pointAccumulationJpaRepository.deleteAll()
    }
    
    "포인트 키로 조회할 수 있어야 한다" {
        // given
        val entity = createPointAccumulationEntity(
            pointKey = "TEST1234",
            memberId = 1L,
            amount = BigDecimal("1000"),
            availableAmount = BigDecimal("1000")
        )
        val saved = pointAccumulationJpaRepository.save(entity)
        
        // when
        val found = pointAccumulationJpaRepository.findByPointKey("TEST1234")
        
        // then
        found.isPresent shouldBe true
        found.get().id shouldBe saved.id
        found.get().pointKey shouldBe "TEST1234"
    }
    
    "존재하지 않는 포인트 키로 조회 시 empty를 반환해야 한다" {
        // when
        val found = pointAccumulationJpaRepository.findByPointKey("NOTEXIST")
        
        // then
        found.isPresent shouldBe false
    }
    
    "회원 ID와 상태로 조회할 수 있어야 한다" {
        // given
        val entity1 = createPointAccumulationEntity(
            pointKey = "KEY1",
            memberId = 1L,
            status = PointAccumulationStatus.ACCUMULATED
        )
        val entity2 = createPointAccumulationEntity(
            pointKey = "KEY2",
            memberId = 1L,
            status = PointAccumulationStatus.ACCUMULATED
        )
        val entity3 = createPointAccumulationEntity(
            pointKey = "KEY3",
            memberId = 1L,
            status = PointAccumulationStatus.CANCELLED
        )
        pointAccumulationJpaRepository.saveAll(listOf(entity1, entity2, entity3))
        
        // when
        val found = pointAccumulationJpaRepository.findByMemberIdAndStatus(1L, PointAccumulationStatus.ACCUMULATED)
        
        // then
        found.size shouldBe 2
        found.all { it.status == PointAccumulationStatus.ACCUMULATED } shouldBe true
        found.all { it.memberId == 1L } shouldBe true
    }
    
    "사용 가능한 적립 건을 조회할 수 있어야 한다" {
        // given
        val tomorrow = LocalDate.now().plusDays(1)
        val yesterday = LocalDate.now().minusDays(1)
        
        // 사용 가능한 적립 건 1 (수기 지급, 만료일 짧음)
        val entity1 = createPointAccumulationEntity(
            pointKey = "KEY1",
            memberId = 1L,
            expirationDate = tomorrow,
            isManualGrant = true,
            availableAmount = BigDecimal("1000")
        )
        // 사용 가능한 적립 건 2 (일반, 만료일 짧음)
        val entity2 = createPointAccumulationEntity(
            pointKey = "KEY2",
            memberId = 1L,
            expirationDate = tomorrow.plusDays(1),
            isManualGrant = false,
            availableAmount = BigDecimal("2000")
        )
        // 만료된 적립 건
        val entity3 = createPointAccumulationEntity(
            pointKey = "KEY3",
            memberId = 1L,
            expirationDate = yesterday,
            availableAmount = BigDecimal("3000")
        )
        // 잔액이 없는 적립 건
        val entity4 = createPointAccumulationEntity(
            pointKey = "KEY4",
            memberId = 1L,
            expirationDate = tomorrow,
            availableAmount = BigDecimal("0")
        )
        pointAccumulationJpaRepository.saveAll(listOf(entity1, entity2, entity3, entity4))
        
        // when
        val found = pointAccumulationJpaRepository.findAvailableAccumulationsByMemberId(1L)
        
        // then
        found.size shouldBe 2
        found[0].pointKey shouldBe "KEY1"  // 수기 지급이 우선
        found[1].pointKey shouldBe "KEY2"  // 만료일 짧은 순
        found.all { it.availableAmount > BigDecimal.ZERO } shouldBe true
        found.all { it.expirationDate >= LocalDate.now() } shouldBe true
        found.all { it.status == PointAccumulationStatus.ACCUMULATED } shouldBe true
    }
    
    "사용 가능 금액 합계를 조회할 수 있어야 한다" {
        // given
        val tomorrow = LocalDate.now().plusDays(1)
        
        val entity1 = createPointAccumulationEntity(
            pointKey = "KEY1",
            memberId = 1L,
            expirationDate = tomorrow,
            availableAmount = BigDecimal("1000")
        )
        val entity2 = createPointAccumulationEntity(
            pointKey = "KEY2",
            memberId = 1L,
            expirationDate = tomorrow,
            availableAmount = BigDecimal("2000")
        )
        // 만료된 적립 건은 제외
        val entity3 = createPointAccumulationEntity(
            pointKey = "KEY3",
            memberId = 1L,
            expirationDate = LocalDate.now().minusDays(1),
            availableAmount = BigDecimal("3000")
        )
        pointAccumulationJpaRepository.saveAll(listOf(entity1, entity2, entity3))
        
        // when
        val sum = pointAccumulationJpaRepository.sumAvailableAmountByMemberId(1L)
        
        // then
        sum shouldBe BigDecimal("3000")  // 1000 + 2000만 포함
    }
    
    "다른 회원의 적립 건은 조회되지 않아야 한다" {
        // given
        val tomorrow = LocalDate.now().plusDays(1)
        
        val entity1 = createPointAccumulationEntity(
            pointKey = "KEY1",
            memberId = 1L,
            expirationDate = tomorrow,
            availableAmount = BigDecimal("1000")
        )
        val entity2 = createPointAccumulationEntity(
            pointKey = "KEY2",
            memberId = 2L,
            expirationDate = tomorrow,
            availableAmount = BigDecimal("2000")
        )
        pointAccumulationJpaRepository.saveAll(listOf(entity1, entity2))
        
        // when
        val found = pointAccumulationJpaRepository.findAvailableAccumulationsByMemberId(1L)
        val sum = pointAccumulationJpaRepository.sumAvailableAmountByMemberId(1L)
        
        // then
        found.size shouldBe 1
        found[0].memberId shouldBe 1L
        sum shouldBe BigDecimal("1000")
    }
    
    "적립 건을 저장하고 조회할 수 있어야 한다" {
        // given
        val entity = createPointAccumulationEntity(
            pointKey = "SAVE123",
            memberId = 1L
        )
        
        // when
        val saved = pointAccumulationJpaRepository.save(entity)
        
        // then
        saved.id shouldNotBe null
        val found = pointAccumulationJpaRepository.findById(saved.id!!)
        found.isPresent shouldBe true
        found.get().pointKey shouldBe "SAVE123"
    }
    
}) {
    companion object {
        fun createPointAccumulationEntity(
            pointKey: String,
            memberId: Long,
            amount: BigDecimal = BigDecimal("1000"),
            availableAmount: BigDecimal = BigDecimal("1000"),
            expirationDate: LocalDate = LocalDate.now().plusDays(365),
            isManualGrant: Boolean = false,
            status: PointAccumulationStatus = PointAccumulationStatus.ACCUMULATED
        ): PointAccumulationEntity {
            val entity = PointAccumulationEntity()
            entity.pointKey = pointKey
            entity.memberId = memberId
            entity.amount = amount
            entity.availableAmount = availableAmount
            entity.expirationDate = expirationDate
            entity.isManualGrant = isManualGrant
            entity.status = status
            entity.createdAt = LocalDateTime.now()
            entity.updatedAt = LocalDateTime.now()
            return entity
        }
    }
}
