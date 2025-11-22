package com.musinsa.payments

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * 무신사페이먼츠 포인트 시스템 메인 애플리케이션
 * 
 * 헥사고날 아키텍처를 기반으로 설계되었습니다.
 * - Domain: 도메인 레이어 (com.musinsa.payments.point.domain)
 * - Application: 애플리케이션 레이어 (com.musinsa.payments.point.application)
 * - Infrastructure: 인프라스트럭처 레이어 (com.musinsa.payments.point.infrastructure)
 * - Presentation: 프레젠테이션 레이어 (com.musinsa.payments.point.presentation)
 */
@SpringBootApplication
class MusinsaPaymentsApplication

fun main(args: Array<String>) {
    runApplication<MusinsaPaymentsApplication>(*args)
}
