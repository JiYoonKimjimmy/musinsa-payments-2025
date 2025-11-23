@echo off
REM 무신사페이먼츠 포인트 시스템 실행 스크립트 (Windows)

REM 스크립트 디렉토리로 이동
cd /d "%~dp0\.."

REM JAR 파일 경로
set JAR_FILE=build\libs\musinsa-payments-0.0.1-SNAPSHOT.jar

REM JAR 파일 존재 확인
if not exist "%JAR_FILE%" (
    echo ❌ JAR 파일을 찾을 수 없습니다: %JAR_FILE%
    echo 먼저 다음 명령어로 빌드해주세요:
    echo   gradlew.bat bootJar
    exit /b 1
)

REM Java 버전 확인
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java가 설치되어 있지 않습니다.
    echo Java 21 이상이 필요합니다.
    exit /b 1
)

REM 기본 JVM 옵션
set JVM_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200

REM 애플리케이션 옵션
set APP_OPTS=

REM 환경 변수로 JVM 옵션 오버라이드 가능
if defined JAVA_OPTS (
    set JVM_OPTS=%JAVA_OPTS%
)

REM 포트 오버라이드 (기본값: 8080)
if not defined SERVER_PORT (
    set SERVER_PORT=8080
)
set APP_OPTS=%APP_OPTS% --server.port=%SERVER_PORT%

echo 🚀 무신사페이먼츠 포인트 시스템을 시작합니다...
echo 📦 JAR 파일: %JAR_FILE%
echo ☕ Java 버전:
java -version
echo 🌐 서버 포트: %SERVER_PORT%
echo.

REM 애플리케이션 실행
java %JVM_OPTS% -jar "%JAR_FILE%" %APP_OPTS%

