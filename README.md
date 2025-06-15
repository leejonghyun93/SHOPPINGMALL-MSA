# 📘 MSA 게시판 프로젝트 (Spring Boot 3.3 + Vue 3)

## 📝 프로젝트 소개

Spring Boot 3.3 기반의 **MSA 게시판 시스템**입니다.  
Oracle과 MariaDB를 함께 사용하는 멀티 DB 환경에서 동작하며,  
Spring Cloud, JWT, JPA, MyBatis를 통합하여 사용자 인증 및 게시글 CRUD 기능을 제공합니다.  
프론트엔드는 Vue 3로 구성되어 있으며, API Gateway를 통해 연동됩니다.

---

## 🧪 주요 기능

- 사용자 회원가입, 로그인 (JWT 기반)
- 게시글 등록 / 수정 / 삭제 / 조회
- 사용자별 게시글 필터링
- 회원가입/탈퇴 이벤트는 Kafka로 비동기 전파
- 서비스 간 통신은 Feign + Spring Cloud Bus로 연동
- Spring Cloud Gateway를 통한 라우팅 및 필터링

---

## 🛠️ 기술 스택

| 영역 | 기술 |
|------|------|
| Language | Java 17 |
| Build Tool | Maven (pom.xml) |
| Framework | Spring Boot 3.3 / Spring Cloud 2023.x |
| Frontend | Vue 3 + Vite |
| Database | Oracle 19c / MariaDB 10.x |
| ORM / SQL | JPA + MyBatis |
| Auth | JWT (Access + Refresh) |
| Service Discovery | Eureka |
| Config | Spring Cloud Config (Git 연동) |
| Gateway | Spring Cloud Gateway |
| Message Bus | Spring Cloud Bus (RabbitMQ) |
| Dev Tools | IntelliJ IDEA, Postman

---

## 🧱 마이크로서비스 구조

- 📦 **msa-board**
    - 📂 config-service – Spring Cloud Config Server
    - 📂 eureka-server – Eureka Service Registry
    - 📂 gateway-service – API Gateway (JWT 필터 포함)
    - 📂 auth-service – 로그인/회원가입, JWT 발급
    - 📂 member-service – 사용자 정보 관리
    - 📂 board-service – 게시글 CRUD 처리
    - 📂 ui-service – API 조합 및 프론트엔드용 응답
    - 📜 README.md

## ⚙️ 실행 순서

1. `config-service` 실행 (Git 설정값 먼저 로딩)
2. `eureka-server` 실행 (서비스 등록)
3. `gateway-service` 실행
4. 나머지 서비스들 (auth, member, board, ui)
5. RabbitMQ 컨테이너 실행 (Spring Cloud Bus 사용 시)

---

## 🔑 JWT 인증 흐름

1. 사용자가 `auth-service`에 로그인 요청
2. Access Token / (추후 개발 예정) Refresh Token 발급
3. 이후 모든 요청은 Gateway에 JWT 포함해서 전송
4. Gateway에서 필터를 통해 JWT 유효성 검증

---

## 📡 Kafka 기반 비동기 이벤트 처리

- `auth-service`에서 회원가입 및 탈퇴 요청이 처리되면, Kafka를 통해 관련 이벤트(`member.created`, `member.deleted`)가 발행됩니다.
- `member-service`와 `board-service`는 해당 토픽을 구독하여, 사용자 정보 동기화 및 관련 데이터 정리를 비동기로 처리합니다.
- 이 구조는 서비스 간 결합도를 낮추고, 확장성과 유지보수를 용이하게 합니다.


--- 


## 🧬 DB 스키마 예시

### users 테이블 (공통)
```sql
CREATE TABLE `t_board` (
                         `BNO` int(11) NOT NULL AUTO_INCREMENT,
                         `TITLE` varchar(500) DEFAULT NULL,
                         `content` text DEFAULT NULL,
                         `WRITER` varchar(100) DEFAULT NULL,
                         `PASSWD` varchar(100) DEFAULT NULL,
                         `regdate` datetime(6) DEFAULT NULL,
                         `VIEWCOUNT` int(11) DEFAULT 0,
                         PRIMARY KEY (`BNO`)
)
```

## 🖥️ 프론트엔드(Vue 3)

- `ui-service`를 통해 게시글/사용자 정보를 Vue 3 앱에서 가져와 렌더링
- CORS 문제는 `gateway-service`에서 허용 처리
- JWT는 `Authorization` 헤더로 포함하여 요청

---
## PowerShell로 Zookeeper 실행

```
cd C:\kafka_2.13-3.6.0
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
```
## PowerShell로 Kafka 서버 실행

```
cd C:\kafka_2.13-3.6.0
.\bin\windows\kafka-server-start.bat .\config\server.properties
```
