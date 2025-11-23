package com.musinsa.payments.point.infrastructure.key

import com.musinsa.payments.point.application.port.output.PointKeyGenerator
import com.musinsa.payments.point.domain.valueobject.PointKey
import org.springframework.stereotype.Component
import java.util.*

/**
 * UUID 기반 포인트 키 생성기
 * Infrastructure 레이어에서 PointKeyGenerator 인터페이스를 구현합니다.
 */
@Component
class UUIDPointKeyGenerator : PointKeyGenerator {
    
    override fun generate(): PointKey {
        // UUID 기반 생성 (8자리 대문자)
        val uuid = UUID.randomUUID().toString().replace("-", "")
        val key = uuid.substring(0, 8).uppercase()
        return PointKey.of(key)
    }
}

