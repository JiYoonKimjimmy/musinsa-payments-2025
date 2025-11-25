package com.musinsa.payments.point.application.port.output.config.fixtures

import com.musinsa.payments.point.application.port.output.config.PointConfigHistoryPort
import com.musinsa.payments.point.domain.entity.PointConfigHistory
import java.util.concurrent.atomic.AtomicLong

/**
 * 포인트 설정 변경 이력 포트의 Fake 구현체
 * 메모리 기반 저장소를 사용하여 테스트에 활용합니다.
 */
class FakePointConfigHistoryPort : PointConfigHistoryPort {

    private val idGenerator = AtomicLong(1L)
    private val storageById = mutableMapOf<Long, PointConfigHistory>()
    private val storageByConfigKey = mutableMapOf<String, MutableList<PointConfigHistory>>()

    override fun save(history: PointConfigHistory): PointConfigHistory {
        // ID가 없으면 새로 할당
        if (history.id == null) {
            history.id = idGenerator.getAndIncrement()
        }

        // 저장
        storageById[history.id!!] = history

        // configKey로 조회할 수 있도록 인덱스 업데이트
        val histories = storageByConfigKey.getOrPut(history.configKey) { mutableListOf() }
        histories.add(history)

        return history
    }

    override fun findByConfigKey(configKey: String): List<PointConfigHistory> {
        // 변경일시 내림차순으로 정렬하여 반환
        return storageByConfigKey[configKey]
            ?.sortedByDescending { it.changedAt }
            ?: emptyList()
    }

    /**
     * 테스트 헬퍼: 저장소 초기화
     */
    fun clear() {
        storageById.clear()
        storageByConfigKey.clear()
        idGenerator.set(1L)
    }

    /**
     * 테스트 헬퍼: 저장된 모든 이력 조회
     */
    fun findAll(): List<PointConfigHistory> {
        return storageById.values.toList()
    }
}
