# 08. AWS 아키텍처 설계

## 1. 개요

무료 포인트 시스템을 AWS 기반으로 서비스한다고 가정했을 때의 아키텍처를 설계합니다. 확장성, 가용성, 보안을 고려한 구성입니다.

## 2. 아키텍처 개요

### 2.1 아키텍처 스타일

- **마이크로서비스 아키텍처**: 포인트 시스템을 독립적인 서비스로 구성
- **서버리스 옵션**: 일부 기능은 서버리스로 구성 가능
- **컨테이너 기반**: ECS/EKS를 활용한 컨테이너 오케스트레이션

### 2.2 주요 AWS 서비스

- **컴퓨팅**: ECS (Fargate) 또는 EC2
- **데이터베이스**: RDS (PostgreSQL/MySQL) 또는 Aurora
- **캐싱**: ElastiCache (Redis)
- **로드 밸런싱**: Application Load Balancer (ALB)
- **API 게이트웨이**: API Gateway (옵션)
- **모니터링**: CloudWatch
- **보안**: VPC, Security Group, IAM

## 3. 아키텍처 다이어그램

### 3.1 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        Internet                              │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                    Route 53 (DNS)                            │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────┐
│              Application Load Balancer (ALB)                 │
│              - SSL/TLS Termination                           │
│              - Health Check                                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        │                                     │
        ↓                                     ↓
┌─────────────────────┐            ┌─────────────────────┐
│   ECS Service       │            │   ECS Service        │
│   (Availability     │            │   (Availability      │
│    Zone 1)          │            │    Zone 2)           │
│                     │            │                      │
│  ┌───────────────┐  │            │  ┌───────────────┐   │
│  │  Fargate      │  │            │  │  Fargate      │   │
│  │  Task         │  │            │  │  Task         │   │
│  │  (Spring Boot)│  │            │  │  (Spring Boot)│   │
│  └───────────────┘  │            │  └───────────────┘   │
└─────────┬───────────┘            └──────────┬──────────┘
          │                                   │
          └──────────────┬────────────────────┘
                         │
          ┌──────────────┴──────────────┐
          │                             │
          ↓                             ↓
┌─────────────────────┐      ┌─────────────────────┐
│   ElastiCache       │      │   RDS (Aurora)       │
│   (Redis)           │      │   (Primary)          │
│   - Config Cache    │      │   - Point Data       │
│   - Session Cache   │      │   - Config Data      │
└─────────────────────┘      └──────────┬──────────┘
                                        │
                                        ↓
                              ┌─────────────────────┐
                              │   RDS (Aurora)      │
                              │   (Replica)         │
                              │   - Read Replica    │
                              └─────────────────────┘
```

### 3.2 네트워크 구성

```
┌─────────────────────────────────────────────────────────────┐
│                        VPC                                   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          Public Subnet (AZ-1)                         │  │
│  │  - ALB                                                 │  │
│  │  - NAT Gateway                                         │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          Private Subnet (AZ-1)                        │  │
│  │  - ECS Tasks                                          │  │
│  │  - ElastiCache                                        │  │
│  │  - RDS (Primary)                                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          Public Subnet (AZ-2)                         │  │
│  │  - NAT Gateway                                         │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          Private Subnet (AZ-2)                        │  │
│  │  - ECS Tasks                                          │  │
│  │  - RDS (Replica)                                      │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 4. 컴포넌트 상세 설계

### 4.1 컴퓨팅 레이어

#### 4.1.1 ECS (Elastic Container Service) with Fargate

**선택 이유**:
- 서버 관리 불필요
- 자동 스케일링
- 컨테이너 기반 배포

**구성**:
- **Task Definition**: Spring Boot 애플리케이션 컨테이너
- **Service**: ECS 서비스 (다중 AZ 배포)
- **Auto Scaling**: CPU/메모리 기반 자동 스케일링

**리소스 사양**:
- CPU: 0.5 vCPU ~ 2 vCPU (트래픽에 따라 조정)
- Memory: 1GB ~ 4GB
- Task Count: 최소 2개, 최대 10개

#### 4.1.2 컨테이너 이미지

- **Base Image**: Amazon Linux 2 또는 OpenJDK 21
- **애플리케이션**: Spring Boot 3.x JAR
- **Health Check**: `/actuator/health` 엔드포인트

### 4.2 데이터베이스 레이어

#### 4.2.1 Amazon RDS (Aurora PostgreSQL)

**선택 이유**:
- 고가용성 (Multi-AZ)
- 자동 백업 및 복구
- 읽기 전용 복제본 지원
- 성능 최적화

**구성**:
- **Primary Instance**: Multi-AZ 배포
- **Read Replica**: 읽기 전용 복제본 (옵션)
- **Backup**: 자동 백업 (7일 보관)
- **Storage**: SSD (GP3)

**인스턴스 사양**:
- **Primary**: db.t3.medium (2 vCPU, 4GB RAM)
- **Replica**: db.t3.small (2 vCPU, 2GB RAM)

#### 4.2.2 데이터베이스 연결 풀

- **HikariCP**: 커넥션 풀 관리
- **Max Pool Size**: 20
- **Min Idle**: 5

### 4.3 캐싱 레이어

#### 4.3.1 Amazon ElastiCache (Redis)

**용도**:
- 포인트 설정 캐싱
- 세션 캐싱 (옵션)
- 분산 락 (옵션)

**구성**:
- **Engine**: Redis 7.x
- **Node Type**: cache.t3.micro (개발) / cache.t3.small (운영)
- **Multi-AZ**: 활성화 (운영 환경)

### 4.4 로드 밸런싱

#### 4.4.1 Application Load Balancer (ALB)

**기능**:
- HTTP/HTTPS 로드 밸런싱
- SSL/TLS 종료
- Health Check
- Path-based 라우팅 (옵션)

**구성**:
- **Listener**: HTTPS (443), HTTP (80 → HTTPS 리다이렉트)
- **Target Group**: ECS 서비스
- **Health Check**: `/actuator/health`

### 4.5 네트워크 보안

#### 4.5.1 VPC 구성

- **CIDR**: 10.0.0.0/16
- **Public Subnet**: 10.0.1.0/24, 10.0.2.0/24
- **Private Subnet**: 10.0.10.0/24, 10.0.20.0/24

#### 4.5.2 Security Group

**ALB Security Group**:
- Inbound: 443 (HTTPS) from 0.0.0.0/0
- Outbound: All traffic

**ECS Security Group**:
- Inbound: 8080 from ALB Security Group
- Outbound: All traffic

**RDS Security Group**:
- Inbound: 5432 (PostgreSQL) from ECS Security Group
- Outbound: None

**ElastiCache Security Group**:
- Inbound: 6379 (Redis) from ECS Security Group
- Outbound: None

## 5. 확장성 설계

### 5.1 수평 확장 (Horizontal Scaling)

#### 5.1.1 ECS Auto Scaling

**트리거**:
- CPU 사용률 > 70%: 스케일 아웃
- CPU 사용률 < 30%: 스케일 인
- 메모리 사용률 > 80%: 스케일 아웃

**정책**:
- 최소 태스크: 2개
- 최대 태스크: 10개
- 스케일 아웃 단계: +2
- 스케일 인 단계: -1

#### 5.1.2 데이터베이스 읽기 확장

- **Read Replica**: 읽기 전용 쿼리는 복제본으로 라우팅
- **Connection Pool**: 읽기/쓰기 분리

### 5.2 수직 확장 (Vertical Scaling)

- **ECS Task**: CPU/메모리 증가
- **RDS Instance**: 인스턴스 타입 업그레이드

## 6. 가용성 설계

### 6.1 Multi-AZ 배포

- **ECS Service**: 여러 가용 영역에 태스크 배포
- **RDS**: Multi-AZ 활성화
- **ElastiCache**: Multi-AZ 활성화 (운영)

### 6.2 Health Check

- **ALB Health Check**: `/actuator/health`
- **ECS Task Health Check**: 컨테이너 레벨 헬스 체크
- **RDS**: 자동 페일오버

### 6.3 백업 및 복구

- **RDS Automated Backups**: 7일 보관
- **Point-in-Time Recovery**: 활성화
- **Snapshot**: 수동 스냅샷 생성 (주기적)

## 7. 보안 설계

### 7.1 네트워크 보안

- **VPC**: 격리된 네트워크 환경
- **Security Group**: 최소 권한 원칙
- **NAT Gateway**: 프라이빗 서브넷의 아웃바운드 트래픽

### 7.2 데이터 보안

- **암호화 전송**: TLS 1.2 이상
- **암호화 저장**: RDS 암호화 활성화
- **Secrets Manager**: 데이터베이스 비밀번호 관리

### 7.3 접근 제어

- **IAM Role**: ECS 태스크에 IAM 역할 할당
- **Least Privilege**: 최소 권한 원칙

## 8. 모니터링 및 로깅

### 8.1 CloudWatch

#### 8.1.1 메트릭

- **ECS**: CPU, Memory, Task Count
- **RDS**: CPU, Memory, Connection Count, Read/Write Latency
- **ElastiCache**: CPU, Memory, Cache Hits/Misses
- **ALB**: Request Count, Response Time, Error Rate

#### 8.1.2 알람

- **High CPU Usage**: CPU > 80% 지속 5분
- **High Memory Usage**: Memory > 85% 지속 5분
- **Database Connection**: Connection Count > 80% 지속 5분
- **Error Rate**: 4xx/5xx > 5% 지속 5분

### 8.2 로깅

- **CloudWatch Logs**: 애플리케이션 로그 수집
- **Log Retention**: 30일
- **Structured Logging**: JSON 형식

## 9. 비용 최적화

### 9.1 리소스 최적화

- **Reserved Instances**: RDS 예약 인스턴스 (1년/3년)
- **Spot Instances**: 개발 환경에서 사용 (옵션)
- **Right Sizing**: 실제 사용량에 맞춘 인스턴스 크기

### 9.2 예상 비용 (월간, 대략적)

- **ECS Fargate**: $50 ~ $200 (트래픽에 따라)
- **RDS Aurora**: $100 ~ $300
- **ElastiCache**: $15 ~ $50
- **ALB**: $20 ~ $30
- **Data Transfer**: $10 ~ $50
- **총 예상 비용**: $195 ~ $630/월

## 10. 배포 전략

### 10.1 CI/CD 파이프라인

```
GitHub/GitLab
    ↓
AWS CodePipeline
    ↓
AWS CodeBuild (빌드 및 테스트)
    ↓
Amazon ECR (컨테이너 이미지 저장)
    ↓
ECS Service Update (Blue/Green 배포)
```

### 10.2 배포 방식

- **Blue/Green 배포**: 무중단 배포
- **Rolling Update**: 점진적 업데이트
- **Canary Deployment**: 일부 트래픽만 새 버전으로 (옵션)

## 11. 대안 아키텍처

### 11.1 서버리스 아키텍처 (옵션)

- **API Gateway**: REST API 엔드포인트
- **Lambda**: 비즈니스 로직 처리
- **DynamoDB**: 데이터 저장 (옵션)
- **장점**: 서버 관리 불필요, 자동 스케일링
- **단점**: 콜드 스타트, 실행 시간 제한

### 11.2 EKS (Kubernetes) 아키텍처 (옵션)

- **EKS**: Kubernetes 클러스터
- **장점**: 더 세밀한 제어, 오픈소스 생태계
- **단점**: 관리 복잡도 증가

## 12. 아키텍처 다이어그램 파일

아키텍처 다이어그램은 다음 도구로 작성하여 `resource/` 폴더에 저장합니다:

- **draw.io**: 온라인 다이어그램 도구
- **Lucidchart**: 클라우드 기반 다이어그램 도구
- **AWS Architecture Icons**: AWS 공식 아이콘 사용

## 13. 다음 단계

다음 단계에서는 테스트 전략을 설계하여 품질 보증 방법을 구체화할 예정입니다.

---

**다음 문서**: [09. 테스트 전략](./09-테스트-전략.md)

