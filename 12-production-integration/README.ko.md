# 12장 - 운영 통합

이 장은 데이터베이스 기반 서비스의 운영형 패턴을 Spring Boot 4와 Ktor로
나란히 비교합니다. 각 예제는 HTTP 계층, 서비스 유스케이스, Exposed 영속성,
테스트, 운영 문서화 경계를 작게 유지하면서도 실제 서비스 구조를 보여줍니다.

## 모듈

| 주제 | Spring Boot 4 | Ktor |
|---|---|---|
| 애플리케이션 아키텍처 | [02-spring-application-architecture](02-spring-application-architecture/README.ko.md) | [01-ktor-application-architecture](01-ktor-application-architecture/README.ko.md) |
| 인증/세션 | issue #59 예정 | issue #59 예정 |
| 리얼타임 아웃박스 | issue #60 예정 | issue #60 예정 |
| HTTP 클라이언트 아웃박스/멱등성 | [03-spring-http-outbox-idempotency](03-spring-http-outbox-idempotency/README.ko.md) | [04-ktor-http-outbox-idempotency](04-ktor-http-outbox-idempotency/README.ko.md) |
| 관측성/준비 상태 | issue #62 예정 | issue #62 예정 |

## 검증

```bash
./gradlew :01-ktor-application-architecture:test
./gradlew :02-spring-application-architecture:test
./gradlew :03-spring-http-outbox-idempotency:test
./gradlew :04-ktor-http-outbox-idempotency:test
```

완료된 주제는 Spring/Ktor 쌍, 집중 테스트, README의 트레이드오프 설명, 실제
외부 서비스에 의존하지 않는 예제 구조를 같은 형태로 유지합니다.
