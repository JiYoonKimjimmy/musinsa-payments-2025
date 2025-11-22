package com.musinsa.payments.point.domain.valueobject

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class OrderNumberTest : StringSpec({
    
    "문자열 값으로 OrderNumber를 생성할 수 있어야 한다" {
        // given & when
        val orderNumber = OrderNumber.of("ORDER1234")
        
        // then
        orderNumber.value shouldBe "ORDER1234"
    }
    
    "빈 문자열로 OrderNumber 생성 시 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            OrderNumber.of("")
        }
    }
    
    "공백만으로 OrderNumber 생성 시 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            OrderNumber.of("   ")
        }
    }
    
    "같은 값의 OrderNumber는 equals로 비교할 수 있어야 한다" {
        // given
        val orderNumber1 = OrderNumber.of("ORDER1234")
        val orderNumber2 = OrderNumber.of("ORDER1234")
        
        // when & then
        orderNumber1 shouldBe orderNumber2
    }
    
    "다른 값의 OrderNumber는 equals로 비교하면 false여야 한다" {
        // given
        val orderNumber1 = OrderNumber.of("ORDER1234")
        val orderNumber2 = OrderNumber.of("ORDER5678")
        
        // when & then
        orderNumber1 shouldNotBe orderNumber2
    }
})

