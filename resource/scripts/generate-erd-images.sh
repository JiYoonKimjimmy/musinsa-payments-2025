#!/bin/bash

# ERD 다이어그램을 이미지로 변환하는 스크립트
# 사용 전에 Mermaid CLI 설치 필요: npm install -g @mermaid-js/mermaid-cli

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESOURCE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$SCRIPT_DIR"

echo "ERD 다이어그램을 이미지로 변환합니다..."

# Mermaid CLI 설치 확인
if ! command -v mmdc &> /dev/null; then
    echo "❌ Mermaid CLI가 설치되어 있지 않습니다."
    echo "다음 명령어로 설치해주세요:"
    echo "  npm install -g @mermaid-js/mermaid-cli"
    exit 1
fi

# PNG 생성
echo "📸 PNG 이미지 생성 중..."
mmdc -i erd.mmd -o "$RESOURCE_DIR/erd.png" -b white -w 2000 -H 1500
if [ $? -eq 0 ]; then
    echo "✅ erd.png 생성 완료"
else
    echo "❌ PNG 생성 실패"
fi

# SVG 생성 (벡터 이미지, 권장)
echo "🎨 SVG 이미지 생성 중..."
mmdc -i erd.mmd -o "$RESOURCE_DIR/erd.svg" -b white
if [ $? -eq 0 ]; then
    echo "✅ erd.svg 생성 완료"
else
    echo "❌ SVG 생성 실패"
fi

# PDF 생성
echo "📄 PDF 문서 생성 중..."
mmdc -i erd.mmd -o "$RESOURCE_DIR/erd.pdf" -b white
if [ $? -eq 0 ]; then
    echo "✅ erd.pdf 생성 완료"
else
    echo "❌ PDF 생성 실패"
fi

echo ""
echo "🎉 이미지 변환 완료!"
echo "생성된 파일:"
ls -lh "$RESOURCE_DIR"/erd.* 2>/dev/null || echo "생성된 파일이 없습니다."

