package com.musinsa.payments.point.infrastructure.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

/**
 * Spring Cache 설정
 * 포인트 설정 조회 성능 최적화를 위한 캐싱을 활성화합니다.
 * Caffeine Cache를 사용하여 TTL, 크기 제한, 성능 최적화 기능을 제공합니다.
 */
@Configuration
@EnableCaching
class CacheConfig {
    
    /**
     * CacheManager 빈 설정
     * CaffeineCacheManager를 사용하여 고성능 인메모리 캐싱을 제공합니다.
     * 
     * 캐시 설정:
     * - TTL: 1시간 (설정은 자주 변경되지 않으므로 충분한 시간)
     * - 최대 크기: 100개 (현재 설정 개수 5개 + 타입별 변환 + 여유분)
     * - Eviction 정책: LRU (Least Recently Used)
     * - 통계 수집: 활성화 (모니터링용)
     */
    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager("pointConfig")
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS) // 1시간 TTL
                .maximumSize(100) // 최대 100개 항목
                .recordStats() // 통계 수집 활성화
        )
        return cacheManager
    }
}

