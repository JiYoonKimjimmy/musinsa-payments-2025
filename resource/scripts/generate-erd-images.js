#!/usr/bin/env node

/**
 * ERD 다이어그램을 이미지로 변환하는 Node.js 스크립트
 * 
 * 사용법:
 *   1. 필요한 패키지 설치:
 *      npm install @mermaid-js/mermaid-cli puppeteer
 * 
 *   2. 스크립트 실행:
 *      node generate-erd-images.js
 */

const { exec } = require('child_process');
const fs = require('fs');
const path = require('path');

const scriptDir = __dirname;
const resourceDir = path.join(scriptDir, '..');
const erdFile = path.join(scriptDir, 'erd.mmd');

console.log('ERD 다이어그램을 이미지로 변환합니다...\n');

// Mermaid CLI 설치 확인
exec('which mmdc', (error, stdout, stderr) => {
  if (error) {
    console.error('❌ Mermaid CLI가 설치되어 있지 않습니다.');
    console.error('다음 명령어로 설치해주세요:');
    console.error('  npm install -g @mermaid-js/mermaid-cli\n');
    process.exit(1);
  }

  // 파일 존재 확인
  if (!fs.existsSync(erdFile)) {
    console.error(`❌ ERD 파일을 찾을 수 없습니다: ${erdFile}`);
    process.exit(1);
  }

  // 이미지 변환 실행
  const formats = [
    { ext: 'png', options: '-b white -w 2000 -H 1500', desc: 'PNG 이미지' },
    { ext: 'svg', options: '-b white', desc: 'SVG 이미지 (벡터, 권장)' },
    { ext: 'pdf', options: '-b white', desc: 'PDF 문서' }
  ];

  formats.forEach((format, index) => {
    const outputFile = path.join(resourceDir, `erd.${format.ext}`);
    const command = `mmdc -i "${erdFile}" -o "${outputFile}" ${format.options}`;

    console.log(`📸 ${format.desc} 생성 중...`);
    
    exec(command, (error, stdout, stderr) => {
      if (error) {
        console.error(`❌ ${format.desc} 생성 실패:`, error.message);
      } else {
        console.log(`✅ erd.${format.ext} 생성 완료`);
      }

      // 마지막 작업이면 완료 메시지 출력
      if (index === formats.length - 1) {
        setTimeout(() => {
          console.log('\n🎉 이미지 변환 완료!');
          console.log('\n생성된 파일:');
          formats.forEach(f => {
            const file = path.join(resourceDir, `erd.${f.ext}`);
            if (fs.existsSync(file)) {
              const stats = fs.statSync(file);
              console.log(`  - erd.${f.ext} (${(stats.size / 1024).toFixed(2)} KB)`);
            }
          });
        }, 500);
      }
    });
  });
});

