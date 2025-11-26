package com.musinsa.payments.point.application.port.output.persistence.fixtures

import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * 포인트 사용 영속성 포트의 Fake 구현체
 * 메모리 기반 저장소를 사용하여 테스트에 활용합니다.
 */
class FakePointUsagePersistencePort : PointUsagePersistencePort {
    
    private val idGenerator = AtomicLong(1L)
    private val storageById = mutableMapOf<Long, PointUsage>()
    private val storageByPointKey = mutableMapOf<String, PointUsage>()
    
    override fun save(usage: PointUsage): PointUsage {
        // ID가 없으면 새로 할당
        if (usage.id == null) {
            usage.id = idGenerator.getAndIncrement()
        }
        
        // 저장
        storageById[usage.id!!] = usage
        storageByPointKey[usage.pointKey] = usage
        
        return usage
    }
    
    override fun findByPointKey(pointKey: String): Optional<PointUsage> {
        return Optional.ofNullable(storageByPointKey[pointKey])
    }
    
    override fun findByMemberIdAndOrderNumber(
        memberId: Long,
        orderNumber: OrderNumber
    ): List<PointUsage> {
        return storageById.values
            .filter { it.memberId == memberId && it.orderNumber == orderNumber }
    }
    
    override fun findUsageHistoryByMemberId(
        memberId: Long,
        orderNumber: String?,
        pageable: Pageable
    ): Page<PointUsage> {
        val filtered = storageById.values
            .filter { it.memberId == memberId }
            .filter { orderNumber == null || it.orderNumber.value == orderNumber }
            .sortedWith(compareByDescending<PointUsage> { it.createdAt })
        
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, filtered.size)
        val pageContent = filtered.subList(start, end)
        
        return PageImpl(pageContent, pageable, filtered.size.toLong())
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
     * 테스트 헬퍼: 저장된 모든 사용 건 조회
     */
    fun findAll(): List<PointUsage> {
        return storageById.values.toList()
    }

    /**
     * 테스트 헬퍼: ID로 사용 건 삭제
     */
    fun deleteById(id: Long) {
        val usage = storageById.remove(id)
        usage?.let {
            storageByPointKey.remove(it.pointKey)
        }
    }
}

