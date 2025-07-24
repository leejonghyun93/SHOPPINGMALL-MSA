# TriMarket 라이브 커머스 사용자 서비스

실시간 방송을 통해 상품을 구매하는 웹사이트 플랫폼

## 프로젝트 개요

### 개발 정보
- **개발 기간**: 2025.06.03 ~ 2025.07.23
- **팀명**: TriCore
- **개발 주제**: 실시간 방송을 위한 상품을 구매하는 웹사이트 플랫폼

### 개발 의도
회원들이 실시간 방송을 통해 상품에 대한 정보를 얻고 자유롭게 의견을 나누며 구매 결정의 도움을 주는 사이트를 구축

### 개발 목적
- Vue.js 를 활용한 SPA 방식의 Front-end 개발
- Spring Boot 와 MariaDB 를 활용한 REST API방식의 Back-end 개발
- Nginx와 Spring Cloud, Docker Container를 활용한 MSA 방식의 웹사이트 플랫폼 제작

## 시스템 아키텍처

### 마이크로서비스 구성
- **Config Service**: 중앙화된 설정 관리, 환경별 구성 정보 제공
- **Eureka Service**: 서비스 디스커버리, 서비스 등록 및 검색
- **Auth Service**: JWT 토큰 생성, 사용자 인증, 소셜 로그인
- **User Service**: 사용자 회원가입, 프로필 관리, 세션 관리
- **Commerce Service**: 상품 관리, 주문 처리, 결제
- **Live Streaming Service**: 실시간 방송 관리
- **Gateway Service**: API 게이트웨이, 라우팅

### 기술 스택

#### Frontend
- Vue.js 3
- HTML5, CSS3, JavaScript ES6+

#### Backend
- Java 17
- Spring Boot
- Spring Cloud Config
- Spring Cloud Eureka
- Spring Cloud Gateway
- Spring Security
- Spring Data JPA
- Spring Batch
- MyBatis

#### Database
- MariaDB

#### 인프라 & 배포
- Nginx (Load Balancer & Reverse Proxy)
- Docker & Docker Compose
- AWS EC2 (운영 서버)
- AWS ECR (컨테이너 레지스트리)
- GitHub Actions (CI/CD)
- Redis (캐싱)

#### 외부 API
- Kakao Map API (주소 검색)
- Kakao Login API (카카오 소셜 로그인)
- Naver Login API (네이버 소셜 로그인)
- Import PG API (결제)
- Gmail SMTP (이메일 알림)

#### 협업 도구
- GitHub
- Figma
- ERD Cloud
- Postman

## 주요 기능

### 사용자 인증
- **회원가입**:
    - 이메일 인증을 통한 일반 회원가입
    - 아이디 중복 확인, 비밀번호 검증
    - 카카오 주소 API 연동으로 주소 입력 간편화

- **로그인**:
    - JWT 토큰 기반 인증 시스템
    - 일반 로그인 (아이디/비밀번호)
    - 소셜 로그인 (카카오, 네이버)

- **JWT 토큰 관리**:
    - Auth Service에서 JWT 토큰 생성 및 검증
    - Access Token + Refresh Token 구조
    - 토큰 만료 시 자동 갱신

- **아이디/비밀번호 찾기**: 이메일을 통한 계정 복구
- **자동 로그인**: Redis 세션 관리 (TTL: 2시간)

### 상품 쇼핑
- **메인 페이지**:
    - Header 컴포넌트 (검색, 로그인 등)
    - Category 컴포넌트 (상품 카테고리별 탐색)
    - Live 컴포넌트 (진행 중인 라이브 방송)
    - Product 컴포넌트 (추천 상품)

- **상품 카테고리**: 계층형 카테고리 구조로 상품 분류
- **상품 상세**: 상품 이미지, 설명, 가격, 리뷰 등 상세 정보
- **장바구니**: 상품 담기 및 수량 조절
- **주문/결제**: Import PG 연동을 통한 안전한 결제

### 라이브 방송
- **방송 목록**: 카테고리별 방송 목록 조회
- **실시간 스트리밍**: HLS 기반 영상 스트리밍
- **실시간 채팅**: WebSocket을 통한 실시간 커뮤니케이션
- **상품 연동**: 방송 중 상품 구매 연결

## 기술적 특징

### MSA 아키텍처
- **서비스 독립성**: 각 서비스별 독립적인 배포 및 확장
- **API Gateway**: Nginx를 통한 요청 라우팅
- **서비스 디스커버리**: Eureka 기반 동적 서비스 검색
- **Config Server**: 중앙화된 설정 관리

### 인증 시스템

### 중앙화된 설정 관리
- **Spring Cloud Config**: 모든 서비스의 설정을 중앙에서 관리
- **환경별 구성**: dev, test, prod 환경별 설정 분리
- **동적 설정 변경**: 서비스 재시작 없이 설정 업데이트
- **Git 기반**: 설정 파일의 버전 관리 및 이력 추적
- **보안 설정**: 민감한 정보 암호화 저장

### 서비스 디스커버리
- **Spring Cloud Eureka**: 마이크로서비스 자동 등록 및 검색
- **동적 서비스 발견**: 서비스 인스턴스의 자동 감지 및 로드밸런싱
- **Health Check**: 서비스 상태 모니터링 및 장애 감지
- **클러스터링**: 고가용성을 위한 Eureka 서버 클러스터 구성

### 배치 처리 시스템
- **Spring Batch**: 대용량 데이터 처리 및 스케줄링
- **주문 데이터 정산**: 일별/월별 매출 데이터 집계
- **사용자 통계**: 사용자 활동 로그 분석 및 리포트 생성
- **데이터 정리**: 오래된 로그 데이터 아카이빙
- **배치 모니터링**: Job 실행 상태 추적 및 실패 알림
- **Spring Data JPA**: 엔티티 기반 객체 관계 매핑
- **MyBatis**: 복잡한 쿼리 및 동적 SQL 처리
- **하이브리드 접근**: JPA와 MyBatis 혼합 사용으로 최적화
- **트랜잭션 관리**: @Transactional 기반 선언적 트랜잭션
- **Auth Service 분리**: 인증 전용 마이크로서비스로 JWT 토큰 관리
- **소셜 로그인**: 카카오, 네이버 OAuth 2.0 연동
- **토큰 기반 인증**: Stateless JWT 토큰으로 확장성 확보
- **보안 강화**: 비밀번호 암호화, 세션 타임아웃 관리
- **Redis 활용**: 사용자 세션, 카테고리 정보 캐싱
- **자동 캐싱**: @Cacheable 어노테이션 활용
- **수동 캐싱**: TTL 기반 캐시 제어

### 데이터 접근 계층
- **RTMP → HLS 변환**: Docker 기반 Nginx RTMP 서버
- **실시간 채팅**: STOMP + SockJS 프로토콜
- **WebSocket 연결**: JWT 기반 인증된 실시간 통신

### 결제 시스템
- **Import PG 연동**: 신뢰성 높은 결제 처리
- **Resilience4j**: Circuit Breaker, Retry, TimeLimiter 적용
- **비동기 처리**: CompletableFuture를 통한 결제 검증 병렬 처리

### 이벤트 기반 아키텍처
- **Apache Kafka**: 서비스 간 비동기 메시지 처리
- **이벤트 드리븐**: 회원 탈퇴, 주문 처리 등 이벤트 기반 처리
- **장애 격리**: 서비스 간 느슨한 결합

### CI/CD 파이프라인
- **GitHub Actions**: 자동화된 빌드 및 배포
- **Multi-stage Build**: Docker 최적화된 이미지 빌드
- **ECR 통합**: AWS Elastic Container Registry 활용
- **무중단 배포**: Blue-Green 배포 방식 적용
- **자동 테스트**: 코드 품질 검사 및 단위 테스트

## 화면 구성

### 메인 페이지
- 헤더 (로그인, 검색, 장바구니)
- 카테고리 네비게이션
- 라이브 방송 목록
- 추천 상품 목록

### 상품 관련
- 상품 카테고리 페이지
- 상품 상세 페이지
- 장바구니 페이지
- 주문서 작성 페이지

### 라이브 방송
- 방송 목록 페이지
- 방송 상세 (영상 + 채팅)
- 방송 중 상품 구매 연결

### 사용자 계정
- 회원가입 페이지 (일반 + 소셜)
- 로그인 페이지 (일반 + 카카오 + 네이버)
- 아이디/비밀번호 찾기
- 마이페이지
- 주문 내역

## 설치 및 실행

### 요구사항
- Node.js 18+
- Java 17+
- Docker & Docker Compose
- MariaDB 10+
- Redis 6+
- AWS CLI (배포용)

### 로컬 개발 환경

#### Frontend 실행
# 소셜 로그인 설정
oauth:
kakao:
client-id: your-kakao-client-id
client-secret: your-kakao-client-secret
redirect-uri: http://localhost:8080/auth/kakao/callback
naver:
client-id: your-naver-client-id
client-secret: your-naver-client-secret
redirect-uri: http://localhost:8080/auth/naver/callback

# Eureka Server 설정
eureka:
server:
enable-self-preservation: false
peer-eureka-nodes-update-interval-ms: 60000
client:
register-with-eureka: false
fetch-registry: false

# Spring Batch 설정
spring:
batch:
jdbc:
initialize-schema: always
job:
enabled: false  # 자동 실행 방지

# 배치 Job 설정
batch:
jobs:
order-settlement:
cron: "0 0 2 * * ?"  # 매일 새벽 2시
user-statistics:
cron: "0 30 1 * * ?"  # 매일 새벽 1시 30분
# MyBatis 설정
mybatis:
mapper-locations: classpath:mappers/*.xml
type-aliases-package: com.trimarket.dto
configuration:
map-underscore-to-camel-case: true
```bash
# 의존성 설치
cd ui-service
npm install

# 환경 변수 설정
cat > .env.local << EOF
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
VITE_KAKAO_CLIENT_ID=your-kakao-client-id
VITE_NAVER_CLIENT_ID=your-naver-client-id
EOF

# 개발 서버 실행
npm run dev

# 빌드
npm run build
```

#### Backend 실행
```bash
# 각 마이크로서비스 빌드
./gradlew build

# Docker 네트워크 생성
docker network create app_msa-network

# 인프라 서비스 먼저 실행
docker-compose up -d mariadb redis rabbitmq zookeeper kafka

# Config Server 및 Eureka 먼저 실행
docker-compose up -d config-service
sleep 45
docker-compose up -d eureka-service
sleep 60

# MSA 서비스들 실행
docker-compose up -d auth-service user-service commerce-service live-streaming-service
sleep 90
docker-compose up -d apigateway-service user-nginx
```

### 운영 환경 배포

#### AWS 인프라 설정
```bash
# EC2 인스턴스 생성 (Amazon Linux 2)
# 보안 그룹: 80, 443, 8080, 8761, 9090, 3000 포트 오픈

# ECR 리포지토리 생성
aws ecr create-repository --repository-name config-service
aws ecr create-repository --repository-name eureka-service
aws ecr create-repository --repository-name auth-service
aws ecr create-repository --repository-name user-service
aws ecr create-repository --repository-name commerce-service
aws ecr create-repository --repository-name live-streaming-service
aws ecr create-repository --repository-name apigateway-service
aws ecr create-repository --repository-name user-nginx
```

#### GitHub Secrets 설정
```bash
# AWS 인증
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
ECR_REGISTRY

# EC2 접속
EC2_HOST
EC2_USERNAME
EC2_SSH_KEY

# 데이터베이스
DB_ROOT_PASSWORD
PROD_DB_URL
PROD_DB_USERNAME
PROD_DB_PASSWORD

# JWT & 보안
PROD_JWT_SECRET_KEY

# 소셜 로그인
PROD_KAKAO_CLIENT_ID
PROD_KAKAO_CLIENT_SECRET
PROD_NAVER_CLIENT_ID
PROD_NAVER_CLIENT_SECRET
PROD_SOCIAL_REDIRECT_URI

# 이메일
PROD_MAIL_USERNAME
PROD_MAIL_PASSWORD
PROD_NOTIFICATION_FROM_EMAIL
```

#### 자동 배포 프로세스
1. **코드 푸시**: main 브랜치에 푸시
2. **변경 감지**: GitHub Actions가 변경된 서비스 감지
3. **이미지 빌드**: ECR에 Docker 이미지 빌드 및 푸시
4. **EC2 배포**: Rolling Update 방식으로 무중단 배포
5. **헬스 체크**: 각 서비스 상태 확인

### 환경 설정

## 배포 아키텍처

### Production 환경
- **서버**: AWS EC2 (Amazon Linux 2)
- **도메인**: http://13.209.253.241
- **컨테이너**: Docker Compose 기반 멀티 컨테이너
- **로드밸런서**: Nginx Reverse Proxy
- **모니터링**: Prometheus + Grafana

### 서비스 포트 구성
```bash
# 사용자 서비스
Frontend (Nginx)     : 80
API Gateway         : 8080

# MSA 서비스들
Config Service      : 8888
Eureka Service      : 8761
Auth Service        : 8082
User Service        : 8103
Commerce Service    : 8090
Live Streaming      : 8096-8097

# 인프라
MariaDB            : 3306
Redis              : 6379
RabbitMQ           : 5672, 15672
Kafka              : 9092, 9093
Zookeeper          : 2181

# 모니터링
Prometheus         : 9090
Grafana           : 3000
```

## 모니터링

### Prometheus 메트릭
- JVM 메모리 사용량
- HTTP 요청 처리 시간
- 데이터베이스 연결 상태
- 서비스 Health Check

### Grafana 대시보드
- 시스템 리소스 모니터링
- 서비스별 성능 지표
- 사용자 트래픽 분석
- 에러 로그 추적


### Frontend 개발
- Vue.js 컴포넌트 개발
- 사용자 인터페이스 설계
- API 연동

### Backend 개발
- MSA 서비스 개발
- API 설계 및 구현
- 데이터베이스 설계

### 인프라
- Docker 컨테이너화
- Nginx 설정
- AWS 배포

## 트러블 슈팅

### 실시간 스트리밍 지연 문제
- **문제**: HLS 기반 스트리밍에서 10초 이상 지연 발생
- **해결**: Nginx 세그먼트 길이 조정 및 OBS Keyframe 간격 최적화
- **결과**: 지연시간 3-5초로 단축

### JWT 토큰 검증 오류
- **문제**: 서비스 간 JWT 토큰 검증 불일치
- **해결**: Auth Service 중앙화 및 공통 JWT 라이브러리 적용
- **결과**: 일관된 토큰 검증 로직 확보

### 소셜 로그인 콜백 처리
- **문제**: 카카오/네이버 로그인 후 프론트엔드 리다이렉트 문제
- **해결**: 콜백 URL 표준화 및 JWT 토큰 자동 전달
- **결과**: 매끄러운 소셜 로그인 사용자 경험

### Docker 컨테이너 최적화 문제
- **문제**: 메모리 부족으로 서비스 재시작 빈발
- **해결**: JVM 힙 메모리 조정 및 G1GC 적용
- **결과**: 안정적인 서비스 운영 및 메모리 사용량 30% 감소

### Eureka 서비스 등록 실패
- **문제**: 서비스들이 Eureka에 정상적으로 등록되지 않음
- **해결**: Eureka 클라이언트 설정 최적화 및 헬스체크 간격 조정
- **결과**: 모든 서비스의 안정적인 서비스 디스커버리 구현

### Spring Batch Job 실행 오류
- **문제**: 배치 작업 실행 중 메모리 부족 및 데드락 발생
- **해결**: Chunk 크기 조정 및 트랜잭션 격리 레벨 최적화
- **결과**: 대용량 데이터 처리 성능 50% 향상

### GitHub Actions 빌드 시간 단축
- **문제**: 전체 빌드에 15분 이상 소요
- **해결**: 변경 감지 기반 선택적 빌드 및 Docker 캐시 활용
- **결과**: 빌드 시간 5분 이내로 단축

### 서비스 간 통신 장애
- **문제**: 동기식 API 호출로 인한 장애 전파
- **해결**: Kafka 기반 비동기 이벤트 처리 도입
- **결과**: 서비스 독립성 확보 및 안정성 향상

## 향후 개선점

- 재고 서비스 및 배송 서비스 기능 추가
- 다시보기 서비스 추가
- 사용자 페이지 전체 개선과 확장
- 관리자 입장에서의 방송 및 상품 모니터링 등, 전체 관리 기능 추가 계획
- Spring Batch Admin 도입으로 배치 작업 모니터링 강화
