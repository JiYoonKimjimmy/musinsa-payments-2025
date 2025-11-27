package com.musinsa.payments.point.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI (Swagger) 설정 클래스
 */
@Configuration
class OpenApiConfig {
    
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("무신사페이먼츠 포인트 시스템 API")
                    .description("무신사페이먼츠 포인트 적립, 사용, 취소, 조회 API 문서")
                    .version("1.0.0")
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("로컬 개발 서버")
                )
            )
    }
}

