package com.musinsa.payments.point.application.port.output.persistence.fixtures

import com.musinsa.payments.point.application.port.output.persistence.PointUsageDetailPersistencePort
import com.musinsa.payments.point.domain.entity.PointUsageDetail
import java.util.concurrent.atomic.AtomicLong

/**
 * 포인트 사용 상세 영속성 포트의 Fake 구현체
 * 메모리 기반 저장소를 사용하여 테스트에 활용합니다.
 */
class FakePointUsageDetailPersistencePort : PointUsageDetailPersistencePort {
    
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
            
            // 인덱스 업데이트는 테스트에서 사용하지 않으므로 간단하게 구현
            // 실제로는 usagePointKey와 accumulationPointKey를 통해 조회해야 하지만,
            // 테스트에서는 주로 saveAll만 사용하므로 여기서는 생략
            
            detail
        }
    }
    
    override fun findByUsagePointKey(pointKey: String): List<PointUsageDetail> {
        // 테스트에서 사용하지 않으므로 간단하게 구현
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
    fun findAll(): List<PointUsageDetail> {
        return storageById.values.toList()
    }
}

