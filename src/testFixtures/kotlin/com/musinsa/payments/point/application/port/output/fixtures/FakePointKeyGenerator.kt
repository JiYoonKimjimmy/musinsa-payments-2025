package com.musinsa.payments.point.application.port.output.fixtures

import com.musinsa.payments.point.application.port.output.PointKeyGenerator
import com.musinsa.payments.point.domain.valueobject.PointKey
import java.util.concurrent.atomic.AtomicInteger

/**
 * 포인트 키 생성기의 Fake 구현체
 * 테스트에서 예측 가능한 키를 생성하거나 순차적 키를 생성할 수 있습니다.
 */
class FakePointKeyGenerator : PointKeyGenerator {
    
    private val sequentialCounter = AtomicInteger(1)
    private var useSequential = true
    
    /**
     * 순차적 키 생성 모드 설정
     * @param useSequential true면 순차적 키 생성 (POINT-001, POINT-002...), false면 UUID 기반
     */
    fun setSequentialMode(useSequential: Boolean) {
        this.useSequential = useSequential
    }
    
    /**
     * 순차적 카운터 리셋
     */
    fun resetCounter() {
        sequentialCounter.set(1)
    }
    
    override fun generate(): PointKey {
        return if (useSequential) {
            val number = sequentialCounter.getAndIncrement()
            PointKey.of("POINT-${String.format("%03d", number)}")
        } else {
            PointKey.generate()
        }
    }
    
}

