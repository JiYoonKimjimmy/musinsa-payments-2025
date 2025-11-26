package com.musinsa.payments.point.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.lang.reflect.Method
import java.util.concurrent.Executor

/**
 * 비동기 처리 설정
 * 포인트 잔액 이벤트 처리를 위한 비동기 실행 환경을 구성합니다.
 */
@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    companion object {
        const val POINT_EVENT_EXECUTOR = "pointEventExecutor"
    }
    
    /**
     * 포인트 이벤트 처리를 위한 스레드 풀 설정
     */
    @Bean(name = [POINT_EVENT_EXECUTOR])
    fun pointEventExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 10
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("point-event-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)
        executor.initialize()
        return executor
    }
    
    override fun getAsyncExecutor(): Executor {
        return pointEventExecutor()
    }
    
    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return PointEventAsyncExceptionHandler()
    }
    
    /**
     * 비동기 작업 예외 핸들러
     */
    private inner class PointEventAsyncExceptionHandler : AsyncUncaughtExceptionHandler {
        override fun handleUncaughtException(ex: Throwable, method: Method, vararg params: Any?) {
            logger.error(
                "비동기 작업 예외 발생 - 메서드: {}, 파라미터: {}, 예외: {}",
                method.name,
                params.contentToString(),
                ex.message,
                ex
            )
        }
    }
}

