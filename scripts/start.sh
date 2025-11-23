#!/bin/bash

# 무신사페이먼츠 포인트 시스템 실행 스크립트 (Linux/Mac)

# 스크립트 디렉토리로 이동
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# JAR 파일 경로
JAR_FILE="build/libs/musinsa-payments-0.0.1-SNAPSHOT.jar"

# JAR 파일 존재 확인
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR 파일을 찾을 수 없습니다: $JAR_FILE"
    echo "먼저 다음 명령어로 빌드해주세요:"
    echo "  ./gradlew bootJar"
    exit 1
fi

# Java 버전 확인
if ! command -v java &> /dev/null; then
    echo "❌ Java가 설치되어 있지 않습니다."
    echo "Java 21 이상이 필요합니다."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21 이상이 필요합니다. 현재 버전: Java $JAVA_VERSION"
    exit 1
fi

# 기본 JVM 옵션
JVM_OPTS="-Xms512m -Xmx1024m"
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=200"

# 애플리케이션 옵션
APP_OPTS=""

# 환경 변수로 JVM 옵션 오버라이드 가능
if [ -n "$JAVA_OPTS" ]; then
    JVM_OPTS="$JAVA_OPTS"
fi

# 포트 오버라이드 (기본값: 8080)
SERVER_PORT=${SERVER_PORT:-8080}
APP_OPTS="$APP_OPTS --server.port=$SERVER_PORT"

echo "🚀 무신사페이먼츠 포인트 시스템을 시작합니다..."
echo "📦 JAR 파일: $JAR_FILE"
echo "☕ Java 버전: $(java -version 2>&1 | head -n 1)"
echo "🌐 서버 포트: $SERVER_PORT"
echo ""

# 애플리케이션 실행
java $JVM_OPTS -jar "$JAR_FILE" $APP_OPTS

