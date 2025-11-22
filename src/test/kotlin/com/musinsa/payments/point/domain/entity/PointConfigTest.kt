package com.musinsa.payments.point.domain.entity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PointConfigTest : StringSpec({
    
    "PointConfig를 생성할 수 있어야 한다" {
        // given
        val configKey = "MAX_AMOUNT"
        val configValue = "100000"
        
        // when
        val config = PointConfig(
            configKey = configKey,
            configValue = configValue
        )
        
        // then
        config.configKey shouldBe configKey
        config.configValue shouldBe configValue
        config.description shouldBe null
    }
    
    "설명과 함께 PointConfig를 생성할 수 있어야 한다" {
        // given
        val configKey = "MAX_AMOUNT"
        val configValue = "100000"
        val description = "최대 금액"
        
        // when
        val config = PointConfig(
            configKey = configKey,
            configValue = configValue,
            description = description
        )
        
        // then
        config.description shouldBe description
    }
    
    "설정 값을 Long 타입으로 변환할 수 있어야 한다" {
        // given
        val config = PointConfig(
            configKey = "MAX_AMOUNT",
            configValue = "100000"
        )
        
        // when
        val value = config.getLongValue()
        
        // then
        value shouldBe 100000L
    }
    
    "설정 값을 Int 타입으로 변환할 수 있어야 한다" {
        // given
        val config = PointConfig(
            configKey = "MAX_DAYS",
            configValue = "365"
        )
        
        // when
        val value = config.getIntValue()
        
        // then
        value shouldBe 365
    }
    
    "설정 값을 Boolean 타입으로 변환할 수 있어야 한다" {
        // given
        val config = PointConfig(
            configKey = "ENABLED",
            configValue = "true"
        )
        
        // when
        val value = config.getBooleanValue()
        
        // then
        value shouldBe true
    }
    
    "숫자가 아닌 값을 Long으로 변환 시 예외가 발생해야 한다" {
        // given
        val config = PointConfig(
            configKey = "MAX_AMOUNT",
            configValue = "invalid"
        )
        
        // when & then
        shouldThrow<IllegalArgumentException> {
            config.getLongValue()
        }
    }
    
    "숫자가 아닌 값을 Int로 변환 시 예외가 발생해야 한다" {
        // given
        val config = PointConfig(
            configKey = "MAX_DAYS",
            configValue = "invalid"
        )
        
        // when & then
        shouldThrow<IllegalArgumentException> {
            config.getIntValue()
        }
    }
    
    "설정 값을 업데이트할 수 있어야 한다" {
        // given
        val config = PointConfig(
            configKey = "MAX_AMOUNT",
            configValue = "100000"
        )
        val originalUpdatedAt = config.updatedAt
        Thread.sleep(10)  // updatedAt 변경을 확인하기 위한 지연
        
        // when
        config.updateConfigValue("200000")
        
        // then
        config.configValue shouldBe "200000"
        config.updatedAt.isAfter(originalUpdatedAt) shouldBe true
    }
    
    "빈 값으로 설정 업데이트 시 예외가 발생해야 한다" {
        // given
        val config = PointConfig(
            configKey = "MAX_AMOUNT",
            configValue = "100000"
        )
        
        // when & then
        shouldThrow<IllegalArgumentException> {
            config.updateConfigValue("")
        }
    }
    
    "설정 키가 비어있으면 생성 시 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointConfig(
                configKey = "",
                configValue = "100000"
            )
        }
    }
    
    "설정 값이 비어있으면 생성 시 예외가 발생해야 한다" {
        // when & then
        shouldThrow<IllegalArgumentException> {
            PointConfig(
                configKey = "MAX_AMOUNT",
                configValue = ""
            )
        }
    }
})

