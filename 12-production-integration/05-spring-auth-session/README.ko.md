# Spring Boot 인증 세션

[English](README.md) | 한국어

이 모듈은 12장 인증/세션 예제의 Spring Boot 4 구현입니다. Spring Security
HTTP Basic, 데이터베이스 기반 사용자 조회, 역할 기반 인가, Exposed 기반 세션
메타데이터 저장을 함께 보여줍니다.

## 아키텍처

![Spring Boot auth session Architecture diagram](../../docs/images/readme-diagrams/12-production-integration-05-spring-auth-session-architecture-01.png)

## 학습 포인트

- 익명, 인증 필요, `ADMIN` 전용 엔드포인트를 나누는 `SecurityFilterChain`.
- 인메모리 사용자 대신 Exposed repository를 사용하는 `UserDetailsService`.
- 워크숍 seed 계정의 BCrypt 비밀번호 해시.
- hash된 토큰과 1시간 만료 시각을 H2/Exposed 테이블에 저장하는 세션 메타데이터.
- 누락 credential, 잘못된 credential, 권한 거부, 프로필 조회, 세션 생성/목록
  조회를 검증하는 HTTP 테스트.

## API

| Endpoint | 인증 | 결과 |
|---|---|---|
| `GET /api/public` | 익명 | 공개 상태 |
| `GET /api/profile` | HTTP Basic | 현재 사용자 프로필과 역할 |
| `GET /api/admin` | HTTP Basic + `ADMIN` 역할 | 관리자 전용 상태 |
| `POST /api/sessions` | HTTP Basic | 세션 메타데이터 생성 및 원문 token 1회 반환 |
| `GET /api/sessions` | HTTP Basic | 원문 token 없는 현재 사용자의 활성 세션 메타데이터 목록 |

Seed 계정:

| Username | Password | Roles |
|---|---|---|
| `alice` | `password` | `USER` |
| `admin` | `password` | `USER`, `ADMIN` |

## Spring Boot 4와 Ktor 비교

| 관심사 | Spring Boot 4 모듈 | Ktor 모듈 |
|---|---|---|
| 인증 hook | Spring Security filter chain | Ktor `Authentication` plugin |
| 사용자 조회 | `UserDetailsService` adapter | Ktor Basic provider에서 service 검증 |
| 인가 | matcher 기반 선언형 규칙 | service 계층 역할 검사 |
| 세션 메타데이터 | MVC controller에서 repository 기록 | Ktor route에서 repository 기록 |
| 비밀번호 해시 | Spring Security Crypto의 BCrypt | Spring Security Crypto의 BCrypt |

이 예제는 HTTP Basic과 JSON API만 다루고 브라우저 form flow가 없어서 CSRF를
비활성화합니다. 쿠키 인증 기반 브라우저 mutation을 추가할 때는 CSRF를 다시 켜거나
토큰 기반 보호를 추가하세요.

## 실행

```bash
./gradlew :05-spring-auth-session:bootRun
```

```bash
curl -u alice:password http://localhost:8080/api/profile
curl -u admin:password http://localhost:8080/api/admin
curl -u alice:password -X POST http://localhost:8080/api/sessions
```

## 테스트

```bash
./gradlew :05-spring-auth-session:test
```
