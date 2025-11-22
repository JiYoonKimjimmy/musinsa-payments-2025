package com.musinsa.payments.point.domain.exception

/**
 * 도메인 예외 추상 클래스
 * 모든 도메인 예외의 기본 클래스입니다.
 */
open class PointDomainException(message: String) : RuntimeException(message)
