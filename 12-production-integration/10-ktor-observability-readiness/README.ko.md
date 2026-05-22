# Ktor Observability Readiness

[English](./README.md) | 한국어

이 모듈은 Exposed 기반 HTTP 서비스의 Ktor 3 운영 진단 예제입니다. Spring Boot
4 모듈과 짝을 이루며 readiness, request correlation, structured error,
slow-operation diagnostics를 애플리케이션 코드 안에 명시적으로 둡니다.

## 아키텍처

![10 ktor observability readiness Architecture diagram](../../docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-architecture-01.png)

## 학습 목표

- Actuator 없이 database-backed `/readyz` endpoint를 구현합니다.
- Ktor `CallId`, `CallLogging`, `ContentNegotiation`, `StatusPages`로 운영
  동작을 구성합니다.
- 들어오는 `X-Request-ID`를 sanitize한 뒤 `CallId`로 echo합니다.
- Exposed JDBC repository 뒤에 slow-operation diagnostics를 저장합니다.
- `testApplication`으로 readiness 성공/실패와 structured error response를
  검증합니다.

## 실행

```bash
./gradlew :10-ktor-observability-readiness:run
```

주요 엔드포인트:

- `GET /readyz`
- `GET /diagnostics/operations/import?delayMs=15`
- `GET /diagnostics/operations`

## 테스트

```bash
./gradlew :10-ktor-observability-readiness:test
```

테스트는 readiness 성공/실패, request-id propagation, structured validation
error, repository persistence, slow-operation classification을 검증합니다.

## 운영 참고

- 들어오는 `X-Request-ID`는 echo/persist 전에 120자 이하이며 문자, 숫자,
  `.`, `_`, `:`, `-`만 포함하도록 제한합니다.
- in-memory H2 persistence는 예제를 self-contained로 유지하기 위한 선택입니다.
  실제 배포에서는 같은 readiness contract를 운영 database와 pool sizing에 맞춰
  적용해야 합니다.

## Spring Boot 4 vs Ktor Tradeoffs

Ktor는 production contract를 route/plugin 코드로 드러냅니다. readiness response
shape, degraded state, error mapping이 모두 일반 코드라 조정하기 쉽지만,
Actuator health-group 같은 기본 convention은 없습니다. 짝을 이루는 Spring Boot
모듈은 플랫폼 convention을 사용해 custom code를 줄이고 Kubernetes probe 기본값에
맞춥니다.
