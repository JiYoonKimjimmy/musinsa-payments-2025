package com.musinsa.payments.point.application.port.output.persistence.fixtures

import com.musinsa.payments.point.application.port.output.persistence.PointUsageDetailPersistencePort
import com.musinsa.payments.point.domain.entity.PointUsageDetail
import java.util.concurrent.atomic.AtomicLong

/**
 * 포인트 사용 상세 영속성 포트의 Fake 구현체
 * 메모리 기반 저장소를 사용하여 테스트에 활용합니다.
 */
class FakePointUsageDetailPersistencePort(
    private val usagePersistencePort: FakePointUsagePersistencePort? = null
) : PointUsageDetailPersistencePort {

    private val idGenerator = AtomicLong(1L)
    private val storageById = mutableMapOf<Long, PointUsageDetail>()
    private val storageByUsagePointKey = mutableMapOf<String, MutableList<PointUsageDetail>>()
    private val storageByAccumulationPointKey = mutableMapOf<String, MutableList<PointUsageDetail>>()
    
    override fun saveAll(details: List<PointUsageDetail>): List<PointUsageDetail> {
        return details.map { detail ->
            // ID가 없으면 새로 할당
            if (detail.id == null) {
                detail.id = idGenerator.getAndIncrement()
            }

            // 저장
            storageById[detail.id!!] = detail

            detail
        }
    }

    override fun findByUsagePointKey(pointKey: String): List<PointUsageDetail> {
        // usagePersistencePort를 통해 usage를 찾고, 해당 usageId로 필터링
        if (usagePersistencePort != null) {
            val usage = usagePersistencePort.findByPointKey(pointKey).orElse(null)
            if (usage != null && usage.id != null) {
                return storageById.values.filter { it.pointUsageId == usage.id }
            }
        }

        // usagePersistencePort가 없으면 인덱스 사용
        return storageByUsagePointKey[pointKey] ?: emptyList()
    }
    
    override fun findByAccumulationPointKey(pointKey: String): List<PointUsageDetail> {
        // 테스트에서 사용하지 않으므로 간단하게 구현
        return storageByAccumulationPointKey[pointKey] ?: emptyList()
    }
    
    /**
     * 테스트 헬퍼: 저장소 초기화
     */
    fun clear() {
        storageById.clear()
        storageByUsagePointKey.clear()
        storageByAccumulationPointKey.clear()
        idGenerator.set(1L)
    }
    
    /**
     * 테스트 헬퍼: 저장된 모든 상세 내역 조회
     */
    override fun findAll(): List<PointUsageDetail> {
        return storageById.values.toList()
    }

    /**
     * 테스트 헬퍼: ID로 상세 내역 삭제
     */
    override fun deleteById(id: Long) {
        storageById.remove(id)
    }
}

