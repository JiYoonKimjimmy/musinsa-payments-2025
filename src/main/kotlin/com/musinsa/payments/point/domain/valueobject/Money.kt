package com.musinsa.payments.point.domain.valueobject

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 금액 값 객체
 * 불변 객체로 구현되어 금액 계산 시 타입 안정성을 보장합니다.
 */
class Money private constructor(rawAmount: BigDecimal) {
    companion object {
        val ZERO = Money(BigDecimal.ZERO)
        
        fun of(amount: Long): Money {
            return Money(BigDecimal.valueOf(amount))
        }
        
        fun of(amount: BigDecimal): Money {
            return Money(amount)
        }
    }
    
    val amount: BigDecimal
    
    init {
        require(rawAmount >= BigDecimal.ZERO) { "금액은 0 이상이어야 합니다." }
        this.amount = rawAmount.setScale(0, RoundingMode.DOWN)
    }
    
    fun add(other: Money): Money {
        return Money(amount.add(other.amount))
    }
    
    fun subtract(other: Money): Money {
        return Money(amount.subtract(other.amount))
    }
    
    fun isGreaterThan(other: Money): Boolean {
        return amount > other.amount
    }
    
    fun isGreaterThanOrEqual(other: Money): Boolean {
        return amount >= other.amount
    }
    
    fun isLessThan(other: Money): Boolean {
        return amount < other.amount
    }
    
    fun isLessThanOrEqual(other: Money): Boolean {
        return amount <= other.amount
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Money) return false
        return amount.compareTo(other.amount) == 0
    }
    
    override fun hashCode(): Int {
        return amount.hashCode()
    }
    
    fun toLong(): Long {
        return amount.toLong()
    }
    
    fun toBigDecimal(): BigDecimal {
        return amount
    }
    
    override fun toString(): String {
        return amount.toString()
    }
}
