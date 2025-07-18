# MSA 기반 라이브 커머스 플랫폼

## 프로젝트 개요

실시간 라이브 스트리밍과 전자상거래를 결합한 마이크로서비스 아키텍처 기반의 라이브 커머스 플랫폼입니다. Spring Cloud를 활용하여 확장 가능하고 유연한 분산 시스템을 구축했으며, 실시간 상호작용과 안정적인 결제 시스템을 제공합니다.

## 주요 특징

- **마이크로서비스 아키텍처**: 독립적으로 배포 가능한 서비스들로 구성
- **실시간 라이브 스트리밍**: WebSocket 기반 실시간 방송 및 채팅
- **이벤트 드리븐 아키텍처**: Kafka와 RabbitMQ를 활용한 비동기 메시징
- **서비스 디스커버리**: Eureka를 통한 동적 서비스 등록 및 발견
- **중앙 집중식 설정 관리**: Spring Cloud Config를 통한 설정 관리
- **API Gateway**: 단일 진입점을 통한 라우팅 및 보안
- **Docker 컨테이너화**: 일관된 개발 및 배포 환경
- **CI/CD 자동화**: GitHub Actions를 통한 자동 빌드 및 배포

## 기술 스택

### Backend
- **Java 17**
- **Spring Boot 3.4.4**
- **Spring Cloud 2024.0.1**
- **Spring Security** - JWT 기반 인증/인가
- **Spring Data JPA** - 데이터 접근 계층
- **Spring Cloud Gateway** - API 게이트웨이
- **Spring Cloud Config** - 중앙 설정 관리
- **Netflix Eureka** - 서비스 디스커버리

### Database & Cache
- **MariaDB 10.11** - 메인 데이터베이스
- **Redis 7** - 캐시 및 세션 저장소

### Message Queue
- **Apache Kafka 7.4.0** - 이벤트 스트리밍
- **RabbitMQ 3** - 메시지 브로커
- **Apache Zookeeper** - Kafka 코디네이션

### Frontend
- **Vue.js 3** - 사용자 인터페이스
- **Vite** - 빌드 도구
- **Nginx** - 웹 서버

### Infrastructure
- **Docker & Docker Compose** - 컨테이너화
- **AWS ECR** - 컨테이너 레지스트리
- **AWS EC2** - 클라우드 호스팅
- **GitHub Actions** - CI/CD 파이프라인

### Monitoring
- **Prometheus** - 메트릭 수집
- **Grafana** - 시각화 대시보드

## 마이크로서비스 아키텍처

### 서비스 구성

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │  API Gateway    │    │  Config Server  │
│   (Nginx)       │    │   (Port 8080)   │    │   (Port 8888)   │
│   (Port 80)     │    └─────────────────┘    └─────────────────┘
└─────────────────┘              │                       │
                                 │                       │
         ┌───────────────────────┼───────────────────────┼───────────────┐
         │                       │                       │               │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐   ┌─────────────────┐
│  Auth Service   │    │  User Service   │    │Commerce Service │   │ Live Streaming  │
│   (Port 8082)   │    │   (Port 8103)   │    │   (Port 8090)   │   │   (Port 8096)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘   └─────────────────┘
         │                       │                       │                       │
         └───────────────────────┼───────────────────────┼───────────────────────┘
                                 │                       │
                    ┌─────────────────┐    ┌─────────────────┐
                    │ Eureka Server   │    │  MariaDB + Redis │
                    │   (Port 8761)   │    │ Kafka + RabbitMQ │
                    └─────────────────┘    └─────────────────┘
```

### 서비스별 역할

**Core Services**
- **Config Service (8888)**: 중앙 집중식 설정 관리
- **Eureka Service (8761)**: 서비스 디스커버리 및 등록
- **API Gateway (8080)**: 라우팅, 인증, 로드 밸런싱

**Business Services**
- **Auth Service (8082)**: 사용자 인증 및 JWT 토큰 관리
- **User Service (8103)**: 사용자 정보 관리 및 프로필
- **Commerce Service (8090)**: 상품, 주문, 결제 관리
- **Live Streaming Service (8096)**: 실시간 방송 및 상호작용

**Infrastructure Services**
- **MariaDB (3306)**: 관계형 데이터 저장
- **Redis (6379)**: 캐시 및 세션 저장
- **Kafka (9092)**: 이벤트 스트리밍 및 메시징
- **RabbitMQ (5672, 15672)**: 메시지 큐잉
- **Prometheus (9090)**: 메트릭 수집
- **Grafana (3000)**: 모니터링 대시보드

## 설치 및 실행 방법

### 사전 준비사항

- Docker 20.10+
- Docker Compose 2.0+
- Java 17 (개발 시)
- Node.js 18+ (프론트엔드 개발 시)

### 환경 설정

1. **환경 변수 파일 생성**
```bash
cp .env.example .env
# .env 파일을 편집하여 필요한 환경 변수 설정
```

2. **필수 환경 변수**
```env
# Database
DB_ROOT_PASSWORD=your_root_password
DB_NAME=live_commerce_db
DB_USERNAME=kosa
DB_PASSWORD=your_db_password

# Production Database
PROD_DB_URL=jdbc:mariadb://mariadb:3306/live_commerce_db
PROD_DB_USERNAME=kosa
PROD_DB_PASSWORD=your_prod_password

# Config Server
CONFIG_REPO_GIT_URI=https://github.com/your-org/config-repo.git
CONFIG_SERVER_PASSWORD=your_config_password

# JWT & Security
PROD_JWT_SECRET_KEY=your_jwt_secret_key

# External Services
PROD_MAIL_USERNAME=your_email@domain.com
PROD_MAIL_PASSWORD=your_email_password
PROD_IAMPORT_API_KEY=your_iamport_key
PROD_IAMPORT_API_SECRET=your_iamport_secret
```

### 로컬 개발 환경 실행

1. **저장소 클론**
```bash
git clone https://github.com/your-org/live-commerce-platform.git
cd live-commerce-platform
```

2. **인프라 서비스 시작**
```bash
docker-compose up -d mariadb redis rabbitmq zookeeper kafka
```

3. **핵심 서비스 시작**
```bash
docker-compose up -d config-service eureka-service
```

4. **비즈니스 서비스 시작**
```bash
docker-compose up -d auth-service user-service commerce-service live-streaming-service
```

5. **API Gateway 및 프론트엔드 시작**
```bash
docker-compose up -d apigateway-service user-nginx
```

6. **모니터링 서비스 시작 (선택사항)**
```bash
docker-compose up -d prometheus grafana
```

### 프로덕션 배포

프로덕션 환경은 GitHub Actions를 통해 자동 배포됩니다:

1. **코드 푸시**
```bash
git push origin main
```

2. **자동 빌드 및 배포**
- GitHub Actions가 자동으로 JAR 빌드
- Docker 이미지 생성 및 ECR 푸시
- EC2 인스턴스에 자동 배포

## 주요 기능

### 사용자 관리
- 회원가입 및 로그인 (일반/소셜)
- JWT 기반 인증 시스템
- 사용자 프로필 관리
- 이메일 인증 및 비밀번호 재설정

### 상품 및 주문 관리
- 상품 등록, 수정, 삭제
- 카테고리별 상품 분류
- 장바구니 및 위시리스트
- 주문 처리 및 결제 (아임포트 연동)
- 주문 상태 추적

### 라이브 스트리밍
- 실시간 라이브 방송
- 채팅 시스템
- 시청자 관리
- 방송 중 상품 판매
- 실시간 알림 시스템

### 관리자 기능
- 대시보드 및 통계
- 사용자 관리
- 상품 관리
- 주문 관리
- 시스템 모니터링

## API 문서

### 인증 API
```
POST /api/auth/login          # 로그인
POST /api/auth/register       # 회원가입
POST /api/auth/refresh        # 토큰 갱신
POST /api/auth/logout         # 로그아웃
```

### 사용자 API
```
GET    /api/users/profile     # 프로필 조회
PUT    /api/users/profile     # 프로필 수정
DELETE /api/users/withdraw    # 회원 탈퇴
```

### 상품 API
```
GET    /api/products          # 상품 목록 조회
GET    /api/products/{id}     # 상품 상세 조회
POST   /api/products          # 상품 등록
PUT    /api/products/{id}     # 상품 수정
DELETE /api/products/{id}     # 상품 삭제
```

### 주문 API
```
GET    /api/orders            # 주문 목록 조회
POST   /api/orders            # 주문 생성
GET    /api/orders/{id}       # 주문 상세 조회
PUT    /api/orders/{id}       # 주문 상태 변경
```

### 라이브 스트리밍 API
```
GET    /api/streams           # 방송 목록 조회
POST   /api/streams           # 방송 시작
PUT    /api/streams/{id}      # 방송 정보 수정
DELETE /api/streams/{id}      # 방송 종료
```

## 헬스체크 및 모니터링

### 서비스 상태 확인
```bash
# 개별 서비스 헬스체크
curl http://localhost:8888/actuator/health  # Config Server
curl http://localhost:8761/actuator/health  # Eureka Server
curl http://localhost:8082/actuator/health  # Auth Service
curl http://localhost:8103/actuator/health  # User Service
curl http://localhost:8090/actuator/health  # Commerce Service
curl http://localhost:8096/actuator/health  # Live Streaming Service
curl http://localhost:8080/actuator/health  # API Gateway
```

### 모니터링 대시보드
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Eureka Dashboard**: http://localhost:8761
- **RabbitMQ Management**: http://localhost:15672 (admin/admin123)

### 메모리 부족 시
```bash
# 메모리 사용량 확인
docker stats

# 불필요한 컨테이너 정리
docker system prune -f

# 메모리 제한 조정 (docker-compose.yml)
deploy:
  resources:
    limits:
      memory: 512M  # 필요에 따라 조정
```

## 기여하기

1. Fork 프로젝트
2. Feature 브랜치 생성 (`git checkout -b feature/amazing-feature`)
3. 변경사항 커밋 (`git commit -m 'Add amazing feature'`)
4. 브랜치에 푸시 (`git push origin feature/amazing-feature`)
5. Pull Request 생성

## 팀 정보

- **Backend 개발**: Spring Boot 마이크로서비스
- **Frontend 개발**: Vue.js 3
- **DevOps**: Docker, AWS, GitHub Actions
- **Database**: MariaDB, Redis
- **Message Queue**: Kafka, RabbitMQ