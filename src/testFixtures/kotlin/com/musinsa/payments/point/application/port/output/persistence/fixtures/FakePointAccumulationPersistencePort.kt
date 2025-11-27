package com.musinsa.payments.point.application.port.output.persistence.fixtures

import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.domain.valueobject.Money
import java.time.LocalDate
import java.util.Optional
import java.util.concurrent.atomic.AtomicLong

/**
 * 포인트 적립 영속성 포트의 Fake 구현체
 * 메모리 기반 저장소를 사용하여 테스트에 활용합니다.
 */
class FakePointAccumulationPersistencePort : PointAccumulationPersistencePort {
    
    private val idGenerator = AtomicLong(1L)
    private val storageById = mutableMapOf<Long, PointAccumulation>()
    private val storageByPointKey = mutableMapOf<String, PointAccumulation>()
    
    override fun save(accumulation: PointAccumulation): PointAccumulation {
        // ID가 없으면 새로 할당
        if (accumulation.id == null) {
            accumulation.id = idGenerator.getAndIncrement()
        }
        
        // 저장
        storageById[accumulation.id!!] = accumulation
        storageByPointKey[accumulation.pointKey] = accumulation
        
        return accumulation
    }
    
    override fun saveAll(accumulations: List<PointAccumulation>): List<PointAccumulation> {
        return accumulations.map { save(it) }
    }
    
    override fun findById(id: Long): Optional<PointAccumulation> {
        return Optional.ofNullable(storageById[id])
    }
    
    override fun findByPointKey(pointKey: String): Optional<PointAccumulation> {
        return Optional.ofNullable(storageByPointKey[pointKey])
    }
    
    override fun findByMemberIdAndStatus(
        memberId: Long,
        status: PointAccumulationStatus
    ): List<PointAccumulation> {
        return storageById.values
            .filter { it.memberId == memberId && it.status == status }
    }
    
    override fun findAvailableAccumulationsByMemberId(memberId: Long): List<PointAccumulation> {
        val today = LocalDate.now()
        return storageById.values
            .filter { 
                it.memberId == memberId 
                    && it.status == PointAccumulationStatus.ACCUMULATED
                    && it.hasAvailableAmount()
                    && !it.isExpiredAt(today)
            }
            .sortedWith(
                compareBy<PointAccumulation> { !it.isManualGrant } // 수기 지급 우선
                    .thenBy { it.expirationDate } // 만료일 짧은 순
            )
    }
    
    override fun sumAvailableAmountByMemberId(memberId: Long): Money {
        val today = LocalDate.now()
        return storageById.values
            .filter {
                it.memberId == memberId
                    && it.status == PointAccumulationStatus.ACCUMULATED
                    && it.hasAvailableAmount()
                    && !it.isExpiredAt(today)
            }
            .fold(Money.ZERO) { sum, accumulation ->
                sum.add(accumulation.availableAmount)
            }
    }

    override fun findByIdWithLock(id: Long): Optional<PointAccumulation> {
        // Fake 구현: 실제 락 없이 기존 메서드 재사용
        return findById(id)
    }

    override fun findAvailableAccumulationsByMemberIdWithLock(memberId: Long): List<PointAccumulation> {
        // Fake 구현: 실제 락 없이 기존 메서드 재사용
        return findAvailableAccumulationsByMemberId(memberId)
    }

    /**
     * 테스트 헬퍼: 저장소 초기화
     */
    fun clear() {
        storageById.clear()
        storageByPointKey.clear()
        idGenerator.set(1L)
    }
    
    /**
     * 테스트 헬퍼: 저장된 모든 적립 건 조회
     */
    override fun findAll(): List<PointAccumulation> {
        return storageById.values.toList()
    }

    /**
     * 테스트 헬퍼: ID로 적립 건 삭제
     */
    override fun deleteById(id: Long) {
        val accumulation = storageById.remove(id)
        accumulation?.let {
            storageByPointKey.remove(it.pointKey)
        }
    }
}

