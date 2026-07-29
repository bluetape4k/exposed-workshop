# Issue 53 Database-per-tenant 예제

## 배경

Issue #53은 하나의 shared pool에서 schema를 switching하는 대신 각 tenant를 dedicated
datasource/database로 routing하는 10장 Spring MVC 예제를 요구했다.

## 결정

Closed `TenantId` whitelist, tenant마다 하나의 Hikari pool과 Exposed `Database`를
소유하는 `TenantDatabaseRegistry`, 그리고 default datasource fallback이 없도록 명시적인
`TenantTransaction.execute {}` 호출을 사용한다.

## 결과

English/Korean README, architecture/sequence PNG diagram, isolation, rollback,
config rejection, lifecycle close, servlet-thread context cleanup을 검증하는 focused
test를 갖춘 `10-multi-tenant/05-database-per-tenant-spring-web`를 추가했다. Selected
examples workflow도 새 모듈을 build한다.

## 검증

- `./gradlew :05-database-per-tenant-spring-web:build --stacktrace --continue`
  passed with 14 tests.
- `actionlint .github/workflows/examples.yml` passed.
- `./gradlew projects --quiet` lists `:05-database-per-tenant-spring-web`.
- README scan으로 Architecture Diagram PNG link, Mermaid 없음, 기존 PNG file 존재를
  확인했다.
- Claude Step 6-R rerun:
  `.omx/artifacts/claude-issue-53-code-review-rerun-stdin-6min-20260523002013.md`
  reported `P0=0, P1=0, P2=0`.

## 향후 agent 지침

ThreadLocal cleanup test는 JUnit client thread가 아니라 servlet/filter thread를 관찰해야
한다. Tenant datasource 예제에서는 registry initialization failure가 발생하면
부분적으로 생성된 pool을 닫고 Spring bean shutdown을 명시적으로 wiring한다.
