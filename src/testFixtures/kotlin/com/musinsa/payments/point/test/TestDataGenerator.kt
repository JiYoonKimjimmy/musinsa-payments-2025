package com.musinsa.payments.point.test

import com.musinsa.payments.point.domain.valueobject.PointKey
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 테스트 데이터 생성 유틸리티
 * 테스트에서 랜덤한 테스트 데이터를 생성하기 위한 헬퍼 객체입니다.
 */
object TestDataGenerator {

    /**
     * 랜덤한 회원 ID를 생성합니다.
     * @return 양수 Long 범위의 랜덤 값
     */
    fun randomMemberId(): Long {
        return Random.nextLong(1L, Long.MAX_VALUE)
    }

    /**
     * 타임스탬프 기반의 랜덤한 주문번호를 생성합니다.
     * @return ORDER-{yyyyMMddHHmmss}-{랜덤숫자} 형식의 문자열
     */
    fun randomOrderNumber(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val randomSuffix = Random.nextLong(1000L, 9999L)
        return "ORDER-$timestamp-$randomSuffix"
    }

    /**
     * 랜덤한 포인트 키를 생성합니다.
     * @return ACCUM-{랜덤문자열} 형식의 문자열
     */
    fun randomPointKey(): String {
        val randomString = PointKey.generate().value
        return "ACCUM-$randomString"
    }
}

