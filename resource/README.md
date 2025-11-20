# Resource 디렉토리

이 디렉토리는 프로젝트의 설계 문서 및 다이어그램 파일을 저장합니다.

## 파일 목록

### ERD 다이어그램
- `erd.mmd`: Mermaid 형식의 ERD 다이어그램 소스 파일
- `generate-erd-images.sh`: 이미지 변환 스크립트 (Bash)
- `generate-erd-images.js`: 이미지 변환 스크립트 (Node.js)

## Mermaid 다이어그램을 이미지로 변환하는 방법

### 자동 변환 스크립트 사용 (권장)

#### Bash 스크립트 사용
```bash
# 실행 권한 확인 (이미 설정되어 있음)
cd resource/

# 스크립트 실행
./generate-erd-images.sh
```

#### Node.js 스크립트 사용
```bash
cd resource/

# 필요한 패키지 설치 (최초 1회)
npm install -g @mermaid-js/mermaid-cli

# 스크립트 실행
node generate-erd-images.js
```

스크립트를 실행하면 `erd.png`, `erd.svg`, `erd.pdf` 파일이 자동으로 생성됩니다.

### 수동 변환 방법

#### 방법 1: Mermaid CLI 사용

1. **Mermaid CLI 설치**
   ```bash
   npm install -g @mermaid-js/mermaid-cli
   ```

2. **이미지 변환**
   ```bash
   # PNG로 변환
   mmdc -i erd.mmd -o erd.png

   # SVG로 변환
   mmdc -i erd.mmd -o erd.svg

   # PDF로 변환
   mmdc -i erd.mmd -o erd.pdf
   ```

### 방법 2: 온라인 도구 사용

1. **Mermaid Live Editor** (https://mermaid.live/)
   - 온라인에서 Mermaid 다이어그램을 작성하고 PNG/SVG로 다운로드

2. **GitHub/GitLab**
   - `.md` 파일에 Mermaid 코드 블록을 작성하면 자동으로 렌더링
   - 브라우저에서 스크린샷으로 저장 가능

### 방법 3: VS Code 확장 사용

1. **Mermaid Preview 확장 설치**
   - VS Code에서 "Mermaid Preview" 확장 설치
   - `.mmd` 파일을 열고 미리보기로 확인 후 이미지로 저장

### 방법 4: Draw.io 사용

1. **Draw.io** (https://app.diagrams.net/)
   - Mermaid 파일 내용을 참고하여 수동으로 다이어그램 작성
   - 더 상세한 다이어그램 작성 가능

## 예상 결과물

변환 후 다음 파일들이 생성됩니다:
- `erd.png`: PNG 형식 이미지
- `erd.svg`: SVG 형식 벡터 이미지 (권장)
- `erd.pdf`: PDF 문서

