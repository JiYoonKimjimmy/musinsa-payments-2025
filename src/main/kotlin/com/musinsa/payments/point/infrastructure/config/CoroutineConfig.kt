package com.musinsa.payments.point.infrastructure.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

/**
 * 코루틴 설정
 * 비동기 작업을 위한 코루틴 디스패처를 구성합니다.
 */
@Configuration
class CoroutineConfig {

    companion object {
        /**
         * I/O 작업용 코루틴 스레드 풀 크기
         * - DB 쿼리, 외부 API 호출 등에 사용
         * - CPU 코어 수 * 2 + 알파로 설정 (권장)
         */
        private const val IO_THREAD_POOL_SIZE = 50

        /**
         * CPU 집약적 작업용 스레드 풀 크기
         * - 계산, 데이터 변환 등에 사용
         * - CPU 코어 수만큼 설정 (권장)
         */
        private const val CPU_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors()
    }

    /**
     * I/O 작업용 코루틴 디스패처
     * DB 쿼리, 외부 API 호출 등 I/O bound 작업에 사용
     */
    @Bean(name = ["ioDispatcher"])
    fun ioDispatcher(): CoroutineDispatcher {
        return Executors.newFixedThreadPool(IO_THREAD_POOL_SIZE) { runnable ->
            Thread(runnable, "coroutine-io-").apply {
                isDaemon = true
            }
        }.asCoroutineDispatcher()
    }

    /**
     * CPU 집약적 작업용 코루틴 디스패처
     * 계산, 데이터 변환 등 CPU bound 작업에 사용
     */
    @Bean(name = ["cpuDispatcher"])
    fun cpuDispatcher(): CoroutineDispatcher {
        return Executors.newFixedThreadPool(CPU_THREAD_POOL_SIZE) { runnable ->
            Thread(runnable, "coroutine-cpu-").apply {
                isDaemon = true
            }
        }.asCoroutineDispatcher()
    }
}
