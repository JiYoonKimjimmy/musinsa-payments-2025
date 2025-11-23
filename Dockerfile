# 무신사페이먼츠 포인트 시스템 Dockerfile
# Java 21 기반 Spring Boot 애플리케이션

# 빌드 스테이지
FROM gradle:8.14.3-jdk21 AS build

WORKDIR /app

# Gradle 캐시를 활용하기 위해 의존성 파일 먼저 복사
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드
RUN gradle bootJar --no-daemon

# 실행 스테이지
FROM eclipse-temurin:21-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 사용자 생성 (root 권한으로 실행하지 않기 위해)
RUN addgroup -S spring && adduser -S spring -G spring

# 빌드 스테이지에서 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 소유권 변경
RUN chown spring:spring app.jar

# 사용자 전환
USER spring:spring

# 포트 노출
EXPOSE 8080

# 헬스 체크 (Spring Boot Actuator가 있다면 사용 가능)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

