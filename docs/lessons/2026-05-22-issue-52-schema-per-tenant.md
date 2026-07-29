# Issue 52 Schema-per-tenant 예제

## 배경

10장에는 하나의 shared database pool을 사용하고 datasource routing에 기대지 않고
tenant isolation을 증명하는 schema-per-tenant Spring Boot 예제가 필요했다. README
정책도 모든 예제에 committed Architecture Diagram PNG image를 요구한다.

## 결정

Tenant input은 closed whitelist(`acme`, `globex`)로 유지하고, 검증된 schema
identifier(`TENANT_ACME`, `TENANT_GLOBEX`)로 mapping한다. Schema switching은
`TenantTransaction` 안에만 두고, 모든 transaction을 `PUBLIC`으로 reset한다. Reset이
실패하면 Hikari connection을 evict하기 전에 rollback하고, rollback/eviction secondary
failure는 `addSuppressed`로 보존한다. 성공한 작업 이후 reset failure도 schema leakage를
막기 위해 rollback-worthy로 취급한다.

## 결과

Servlet header validation, Exposed JDBC repository access, schema setup/seed data,
failure handling, README pair, committed SVG+PNG diagram, examples workflow coverage를
갖춘 `10-multi-tenant/04-schema-per-tenant-spring-web`를 추가했다. Chapter와 root
README index도 새 모듈을 포함한다.

## 검증

- `./gradlew :04-schema-per-tenant-spring-web:test --stacktrace --continue`
  passed with 15 tests.
- Architecture/sequence PNG는 SVG에서 rendering한 뒤 직접 열어 light box 위 label이
  어둡고 읽을 수 있음을 확인했다.
- Spec/plan Claude advisor gate passed with `P0=0`, `P1=0` in
  `.omx/artifacts/claude-issue-52-spec-plan-advisor-stdin-6min-20260522215325.md`.

## 향후 참고

Physical connection reuse의 증거로 Hikari proxy object identity를 사용하지 않는다. Pool에
physical connection이 하나뿐이어도 checkout proxy는 달라질 수 있다. H2 예제에서
same-session evidence가 필요하면 `SESSION_ID()`를 기록한다.
