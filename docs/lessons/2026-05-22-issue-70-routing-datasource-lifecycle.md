# Issue 70 Routing datasource lifecycle

## 배경

Issue #70은 11장 routing datasource 예제가 tenant Hikari pool을 만들지만 Spring에
deterministic shutdown hook을 주지 않는다고 보고했다.

## 결정

`DataSourceRegistry`를 `AutoCloseable`로 만들고, `InMemoryDataSourceRegistry`가 현재
등록된 closeable data source의 shutdown을 소유하게 한다. 여러 key 아래 등록된 shared
datasource가 한 번만 close되도록 identity 기준으로 deduplicate한다. 기존 key를 교체해도 old
datasource를 닫지 않는다는 점을 문서화한다.

## 결과

`RoutingDataSourceConfig`의 TODO를 제거했다. Routing datasource 예제는 이제 Spring
shutdown 중 registry-owned Hikari pool을 닫고, `Examples.yml`은 selected chapter 12 예제와
함께 routing module을 build한다. Routing module의 GraalVM AOT task가 local
configuration-cache serialization failure를 유발했기 때문에 이 mixed example build에서는
Gradle configuration cache를 비활성화한다.

## 검증

- `actionlint .github/workflows/examples.yml`
- `./gradlew :03-routing-datasource:test --no-daemon` — 29 passing
- `./gradlew :03-routing-datasource:build :01-ktor-application-architecture:build :02-spring-application-architecture:build :03-spring-http-outbox-idempotency:build :04-ktor-http-outbox-idempotency:build --no-daemon --no-configuration-cache --continue`
- Claude advisor artifact: `.omx/artifacts/ask-claude-code-review-issue-70-routing-datasource-final2-20260522103201.md` — P0=0, P1=0

## 향후 보호 장치

Example-owned pool을 추가할 때 owner를 `AutoCloseable`로 만들고 idempotent close,
shared-instance close-once behavior, suppressed exception propagation을 테스트한다.
