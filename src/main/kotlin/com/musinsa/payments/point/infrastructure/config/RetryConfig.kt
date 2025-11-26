package com.musinsa.payments.point.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry

/**
 * Spring Retry 설정
 * 포인트 잔액 이벤트 처리 실패 시 재시도 기능을 활성화합니다.
 */
@EnableRetry
@Configuration
class RetryConfig

