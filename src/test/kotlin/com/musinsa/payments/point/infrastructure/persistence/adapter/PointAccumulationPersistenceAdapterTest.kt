package com.musinsa.payments.point.infrastructure.persistence.adapter

import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.domain.valueobject.Money
import com.musinsa.payments.point.domain.valueobject.PointKey
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.PointAccumulationJpaRepository
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
 * PointAccumulationPersistenceAdapter 통합 테스트
 * Adapter의 메서드와 도메인-JPA 엔티티 변환을 함께 검증합니다.
 */
@Import(PointEntityMapper::class)
@ActiveProfiles("test")
@DataJpaTest
class PointAccumulationPersistenceAdapterTest @Autowired constructor(
    private val pointAccumulationJpaRepository: PointAccumulationJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : StringSpec({
    
    extensions(SpringExtension)
    
    lateinit var adapter: PointAccumulationPersistenceAdapter
    
    beforeTest {
        pointAccumulationJpaRepository.deleteAll()

        adapter = PointAccumulationPersistenceAdapter(pointAccumulationJpaRepository, pointEntityMapper)
    }
    
    "도메인 엔티티를 저장하고 조회할 수 있어야 한다" {
        // given
        val accumulation = createPointAccumulation(
            pointKey = PointKey.generate(),
            memberId = 1L,
            amount = Money.of(1000L)
        )
        
        // when
        val saved = adapter.save(accumulation)
        
        // then
        saved.id shouldNotBe null
        saved.pointKey shouldBe accumulation.pointKey
        saved.memberId shouldBe 1L
        saved.amount shouldBe Money.of(1000L)
        saved.availableAmount shouldBe Money.of(1000L)
    }
    
    "포인트 키로 도메인 엔티티를 조회할 수 있어야 한다" {
        // given
        val accumulation = createPointAccumulation(
            pointKey = PointKey.of("TEST1234"),
            memberId = 1L
        )
        val saved = adapter.save(accumulation)
        
        // when
        val found = adapter.findByPointKey("TEST1234")
        
        // then
        found.isPresent shouldBe true
        found.get().id shouldBe saved.id
        found.get().pointKey shouldBe "TEST1234"
        found.get().memberId shouldBe 1L
    }
    
    "존재하지 않는 포인트 키로 조회 시 empty를 반환해야 한다" {
        // when
        val found = adapter.findByPointKey("NOTEXIST")
        
        // then
        found.isPresent shouldBe false
    }
    
    "회원 ID와 상태로 도메인 엔티티 목록을 조회할 수 있어야 한다" {
        // given
        val accumulation1 = createPointAccumulation(
            pointKey = PointKey.generate(),
            memberId = 1L,
            status = PointAccumulationStatus.ACCUMULATED
        )
        val accumulation2 = createPointAccumulation(
            pointKey = PointKey.generate(),
            memberId = 1L,
            status = PointAccumulationStatus.ACCUMULATED
        )
        val accumulation3 = createPointAccumulation(
            pointKey = PointKey.generate(),
            memberId = 1L,
            status = PointAccumulationStatus.CANCELLED
        )
        adapter.save(accumulation1)
        adapter.save(accumulation2)
        adapter.save(accumulation3)
        
        // when
        val found = adapter.findByMemberIdAndStatus(1L, PointAccumulationStatus.ACCUMULATED)
        
        // then
        found.size shouldBe 2
        found.all { it.status == PointAccumulationStatus.ACCUMULATED } shouldBe true
        found.all { it.memberId == 1L } shouldBe true
    }
    
    "사용 가능한 적립 건을 조회할 수 있어야 한다" {
        // given
        val tomorrow = LocalDate.now().plusDays(1)
        val yesterday = LocalDate.now().minusDays(1)
        
        val accumulation1 = createPointAccumulation(
            pointKey = PointKey.generate(),
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = tomorrow,
            isManualGrant = true,
            availableAmount = Money.of(1000L)
        )
        val accumulation2 = createPointAccumulation(
            pointKey = PointKey.generate(),
            memberId = 1L,
            amount = Money.of(2000L),
            expirationDate = tomorrow.plusDays(1),
            isManualGrant = false,
            availableAmount = Money.of(2000L)
        )
        // 만료된 적립 건은 오늘로 설정하여 생성하고, 저장 후 JPA Entity에서 직접 만료일 변경
        val accumulation3 = createPointAccumulation(
            pointKey = PointKey.generate(),
            memberId = 1L,
            amount = Money.of(3000L),
            expirationDate = LocalDate.now(),  // 오늘로 생성 (검증 통과)
            availableAmount = Money.of(3000L)
        )
        val accumulation4 = createPointAccumulation(
            pointKey = PointKey.generate(),
            memberId = 1L,
            amount = Money.of(1000L),
            expirationDate = tomorrow,
            availableAmount = Money.ZERO  // 잔액 없음
        )
        adapter.save(accumulation1)
        adapter.save(accumulation2)
        val saved3 = adapter.save(accumulation3)
        adapter.save(accumulation4)
        
        // JPA Repository에서 직접 만료일을 과거로 변경
        val entity3 = pointAccumulationJpaRepository.findById(saved3.id!!).get()
        entity3.expirationDate = yesterday  // 어제로 변경
        pointAccumulationJpaRepository.save(entity3)
        
        // when
        val found = adapter.findAvailableAccumulationsByMemberId(1L)
        
        // then
        found.size shouldBe 2
        found[0].isManualGrant shouldBe true  // 수기 지급이 우선
        found.all { it.availableAmount.isGreaterThan(Money.ZERO) } shouldBe true
        found.all { !it.isExpired() } shouldBe true
        found.all { it.status == PointAccumulationStatus.ACCUMULATED } shouldBe true
    }
    
    "사용 가능 금액 합계를 조회할 수 있어야 한다" {
        // given
        val tomorrow = LocalDate.now().plusDays(1)
        
        val accumulation1 = createPointAccumulation(
            pointKey = PointKey.generate(),
            memberId = 1L,
            expirationDate = tomorrow,
            availableAmount = Money.of(1000L)
        )
        val accumulation2 = createPointAccumulation(
            pointKey = PointKey.generate(),
            memberId = 1L,
            amount = Money.of(2000L),
            expirationDate = tomorrow,
            availableAmount = Money.of(2000L)
        )
        // 만료된 적립 건은 오늘로 설정하여 생성하고, 저장 후 JPA Entity에서 직접 만료일 변경
        val accumulation3 = createPointAccumulation(
            pointKey = PointKey.generate(),
            memberId = 1L,
            amount = Money.of(3000L),
            expirationDate = LocalDate.now(),  // 오늘로 생성 (검증 통과)
            availableAmount = Money.of(3000L)
        )
        adapter.save(accumulation1)
        adapter.save(accumulation2)
        val saved3 = adapter.save(accumulation3)
        
        // JPA Repository에서 직접 만료일을 과거로 변경
        val entity3 = pointAccumulationJpaRepository.findById(saved3.id!!).get()
        entity3.expirationDate = LocalDate.now().minusDays(1)  // 어제로 변경
        pointAccumulationJpaRepository.save(entity3)
        
        // when
        val sum = adapter.sumAvailableAmountByMemberId(1L)
        
        // then
        sum shouldBe Money.of(3000L)  // 1000 + 2000만 포함
    }
    
    "저장 시 ID가 자동으로 생성되어야 한다" {
        // given
        val accumulation = createPointAccumulation(
            pointKey = PointKey.generate(),
            memberId = 1L
        )
        accumulation.id shouldBe null  // 저장 전에는 null
        
        // when
        val saved = adapter.save(accumulation)
        
        // then
        saved.id shouldNotBe null
        saved.id!! shouldBe saved.id  // ID가 할당됨
    }
    
    "도메인 엔티티의 모든 필드가 올바르게 저장되고 조회되어야 한다" {
        // given
        val pointKey = PointKey.generate()
        val expirationDate = LocalDate.now().plusDays(365)
        val accumulation = PointAccumulation(
            pointKey = pointKey.value,
            memberId = 1L,
            amount = Money.of(5000L),
            expirationDate = expirationDate,
            isManualGrant = true,
            status = PointAccumulationStatus.ACCUMULATED,
            createdAt = LocalDateTime.now().minusHours(1),
            updatedAt = LocalDateTime.now().minusHours(1)
        )
        
        // when
        val saved = adapter.save(accumulation)
        val found = adapter.findByPointKey(pointKey.value)
        
        // then
        found.isPresent shouldBe true
        val retrieved = found.get()
        retrieved.id shouldBe saved.id
        retrieved.pointKey shouldBe pointKey.value
        retrieved.memberId shouldBe 1L
        retrieved.amount shouldBe Money.of(5000L)
        retrieved.availableAmount shouldBe Money.of(5000L)
        retrieved.expirationDate shouldBe expirationDate
        retrieved.isManualGrant shouldBe true
        retrieved.status shouldBe PointAccumulationStatus.ACCUMULATED
    }
    
}) {
    companion object {
        fun createPointAccumulation(
            pointKey: PointKey,
            memberId: Long,
            amount: Money = Money.of(1000L),
            availableAmount: Money = Money.of(1000L),
            expirationDate: LocalDate = LocalDate.now().plusDays(365),
            isManualGrant: Boolean = false,
            status: PointAccumulationStatus = PointAccumulationStatus.ACCUMULATED
        ): PointAccumulation {
            // availableAmount는 amount 이하여야 함
            val validAvailableAmount = if (availableAmount.isGreaterThan(amount)) {
                amount
            } else {
                availableAmount
            }
            
            val accumulation = PointAccumulation(
                pointKey = pointKey.value,
                memberId = memberId,
                amount = amount,
                expirationDate = expirationDate,
                isManualGrant = isManualGrant,
                status = status
            )
            
            // availableAmount가 amount보다 작은 경우, use 메서드를 통해 조정
            if (validAvailableAmount.isLessThan(amount)) {
                val difference = amount.subtract(validAvailableAmount)
                accumulation.use(difference)
            }
            
            return accumulation
        }
        
    }
}

