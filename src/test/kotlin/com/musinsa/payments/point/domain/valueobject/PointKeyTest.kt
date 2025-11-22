package com.musinsa.payments.point.domain.valueobject

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PointKeyTest : StringSpec({
    
    "문자열 값으로 PointKey를 생성할 수 있어야 한다" {
        // given & when
        val pointKey = PointKey.of("TEST1234")
        
        // then
        pointKey.value shouldBe "TEST1234"
    }
    
    "빈 문자열로 PointKey 생성 시 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointKey.of("")
        }
    }
    
    "공백만으로 PointKey 생성 시 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointKey.of("   ")
        }
    }
    
    "PointKey를 UUID 기반으로 생성할 수 있어야 한다" {
        // given & when
        val pointKey1 = PointKey.generate()
        val pointKey2 = PointKey.generate()
        
        // then
        pointKey1.value.length shouldBe 8
        pointKey2.value.length shouldBe 8
        pointKey1.value shouldNotBe pointKey2.value  // 서로 다른 값이어야 함
        pointKey1.value.all { it.isUpperCase() || it.isDigit() } shouldBe true
    }
    
    "같은 값의 PointKey는 equals로 비교할 수 있어야 한다" {
        // given
        val pointKey1 = PointKey.of("TEST1234")
        val pointKey2 = PointKey.of("TEST1234")
        
        // when & then
        pointKey1 shouldBe pointKey2
    }
    
    "다른 값의 PointKey는 equals로 비교하면 false여야 한다" {
        // given
        val pointKey1 = PointKey.of("TEST1234")
        val pointKey2 = PointKey.of("TEST5678")
        
        // when & then
        pointKey1 shouldNotBe pointKey2
    }
})

