package com.musinsa.payments.point.infrastructure.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring Cache 설정
 * 포인트 설정 조회 성능 최적화를 위한 캐싱을 활성화합니다.
 */
@Configuration
@EnableCaching
class CacheConfig {
    
    /**
     * CacheManager 빈 설정
     * ConcurrentMapCacheManager를 사용하여 인메모리 캐싱을 제공합니다.
     */
    @Bean
    fun cacheManager(): CacheManager {
        return ConcurrentMapCacheManager("pointConfig")
    }
}

