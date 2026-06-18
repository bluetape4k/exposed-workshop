# 12장 - 운영 통합

[English](README.md) | 한국어

이 장은 소스 트리에 있는 예제를 기준으로 운영형 서비스 패턴을 Spring Boot 4와
Ktor에서 나란히 비교합니다. 각 예제는 코드를 읽기 쉬운 크기로 유지하면서도
운영에서 중요한 경계, 즉 HTTP entrypoint, service use case, Exposed persistence,
test, security, outbox delivery, operational diagnostics를 분리해서 보여줍니다.

## 챕터 목표

- Spring Boot 4와 Ktor 중 하나를 기본값으로 놓지 않고, 같은 service pattern을
  두 프레임워크에서 비교한다.
- Exposed persistence를 테스트하기 쉬운 repository boundary 뒤에 둔다.
- authentication, idempotency, realtime delivery, readiness, request
  correlation이 작은 서비스의 어느 지점에 놓이는지 확인한다.
- H2, fake gateway, in-process hub, focused test를 사용해 기본 workshop path에서
  실제 외부 시스템 의존성을 제거한다.

## 아키텍처 다이어그램

![12장 운영 통합 아키텍처](../docs/images/readme-diagrams/12-production-integration-architecture-01.png)

Foundation 예제 01-04는 application architecture와 HTTP outbox/idempotency를
다룹니다. 예제 05-10은 그 위에 authentication/session metadata, realtime outbox
delivery, observability/readiness behavior를 올립니다.

## 모듈

| 주제 | Spring Boot 4 | Ktor |
|---|---|---|
| 애플리케이션 아키텍처 | [02-spring-application-architecture](02-spring-application-architecture/README.ko.md) | [01-ktor-application-architecture](01-ktor-application-architecture/README.ko.md) |
| 인증/세션 | [05-spring-auth-session](05-spring-auth-session/README.ko.md) | [06-ktor-auth-session](06-ktor-auth-session/README.ko.md) |
| 리얼타임 아웃박스 | [07-spring-outbox-realtime](07-spring-outbox-realtime/README.ko.md) | [08-ktor-outbox-realtime](08-ktor-outbox-realtime/README.ko.md) |
| HTTP 클라이언트 아웃박스/멱등성 | [03-spring-http-outbox-idempotency](03-spring-http-outbox-idempotency/README.ko.md) | [04-ktor-http-outbox-idempotency](04-ktor-http-outbox-idempotency/README.ko.md) |
| 관측성/준비 상태 | [09-spring-observability-readiness](09-spring-observability-readiness/README.ko.md) | [10-ktor-observability-readiness](10-ktor-observability-readiness/README.ko.md) |

## 검증

```bash
./gradlew :01-ktor-application-architecture:test
./gradlew :02-spring-application-architecture:test
./gradlew :03-spring-http-outbox-idempotency:test
./gradlew :04-ktor-http-outbox-idempotency:test
./gradlew :05-spring-auth-session:test
./gradlew :06-ktor-auth-session:test
./gradlew :07-spring-outbox-realtime:test
./gradlew :08-ktor-outbox-realtime:test
./gradlew :09-spring-observability-readiness:test
./gradlew :10-ktor-observability-readiness:test
```

각 예제는 필요한 경우 Spring/Ktor 쌍, focused test, 생성된 PNG/SVG architecture
asset, 명시적인 tradeoff, 기본 workshop path에서 실제 외부 서비스에 의존하지
않는 구조를 같은 형태로 유지합니다.

## 검증 범위

- `settings.gradle.kts`는 `includeModules("12-production-integration", false,
  false)`로 chapter module을 포함하므로, 새 예제 디렉터리는 모듈별 settings
  수정 없이 발견됩니다.
- `.github/workflows/examples.yml`은 12장 변경과 일일 schedule에서 선택된
  12장 모듈 01-10을 build합니다.
- `.github/workflows/ci.yml`은 비문서 변경이 full CI를 요구할 때 H2,
  PostgreSQL, MySQL 8, MariaDB test matrix를 실행합니다.
- `.github/workflows/nightly.yml`은 이미 full H2 test task와 선택된 DB shard를
  실행합니다. 이 self-contained 예제들은 향후 외부 인프라가 필요한 예제가
  추가되지 않는 한 별도 nightly override가 필요하지 않습니다.
