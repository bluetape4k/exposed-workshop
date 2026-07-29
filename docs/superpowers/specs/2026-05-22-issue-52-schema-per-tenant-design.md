# Issue 52 Schema-per-tenant 설계

## 배경

Issue #52는 shared database infrastructure에서 tenant-specific schema routing을 보여 주는 10장
Spring Boot 예제를 추가한다. 예제는 strict tenant resolution, schema switching,
tenant-local persistence, cross-tenant isolation에 집중해야 한다.

## 범위

- `10-multi-tenant/04-schema-per-tenant-spring-web`를 추가한다.
- `X-Tenant-ID`에서 tenant를 resolve하고 missing/unknown tenant를 명시적으로 처리한다.
- 하나의 shared Hikari datasource와 Exposed JDBC transaction을 사용한다.
- Tenant마다 하나의 schema를 만들고 tenant schema를 선택한 뒤 table operation을 실행한다.
- Missing tenant, unknown tenant, valid tenant routing, isolation을 테스트한다.
- README pair와 committed Architecture Diagram PNG/SVG를 추가한다.

## 설계

모듈은 strict servlet filter로 request-local `TenantContext`를 설정한다. 이전 10장 baseline
module과 달리 missing tenant header는 default tenant로 fallback하지 않고 HTTP 400으로 실패한다.
Repository operation은 shared `InventoryItems` table을 건드리기 전에 하나의
`tenantTransaction { }` wrapper를 사용한다. Repository는 ad hoc schema switching call을 내면
안 된다.

모든 Exposed transaction은 시작 시 명시적으로 `SET SCHEMA`를 수행해야 한다. 어떤 코드도 pooled
connection의 이전 schema state에 암묵적으로 의존하면 안 된다. Helper는 transaction 반환 전에
schema를 `PUBLIC`으로 reset하여 Hikari connection reuse가 이전 tenant schema를 다음 borrower에게
노출하지 못하게 한다. Schema switching은 Exposed `Schema` object와 `SchemaUtils.setSchema`를
사용해야 한다. Whitelisted tenant value라도 `exec("SET SCHEMA $name")` 같은 raw SQL은 금지한다.
Mapped schema identifier는 `Schema` object를 만들기 전에 `^[A-Z_][A-Z0-9_]{0,63}$`와
일치해야 한다.

`PUBLIC` reset이 실패하면 underlying JDBC connection을 healthy 상태로 pool에 조용히 반환하면
안 된다. Reset-failure path는 failure를 다시 던지기 전에
`HikariDataSource.evictConnection(connection)` 또는 동등한 pool-level eviction hook으로 현재
Hikari connection을 evict해야 한다. Test는 reset failure 처리 후 나중 tenant request가 이전
tenant schema를 상속할 수 없음을 증명해야 한다. Helper는 현재 Exposed transaction에서
`TransactionManager.current().connection.connection as java.sql.Connection`으로 physical JDBC
connection을 얻는다. 이는 unwrapped physical driver connection이 아니라 Exposed에 전달된
Hikari-borrowed proxy connection이다. `tenantTransaction`은 다음 shape를 사용해야 한다.
`transaction { try { SchemaUtils.setSchema(tenantSchema); block() } finally { resetter.resetToPublic(proxyConnection) } }`.
따라서 reset은 Exposed가 commit, rollback, release를 수행하기 전 active Exposed transaction에
connection이 아직 bound된 상태에서 실행된다. Reset behavior는 `SchemaResetter` seam 뒤에
구현하여 test가 failing reset을 inject하고 connection eviction hook 호출을 assert할 수 있게
한다. Reset이 실패하면 구현은 Hikari-borrowed proxy connection을 evict하기 전에 현재
transaction을 rollback으로 표시하거나 active transaction에 rollback을 호출해야 한다. 이후
Exposed가 secondary commit/rollback/connection-closed error를 발생시키면 original reset failure가
primary exception으로 남아야 하며 secondary failure는 `addSuppressed`로 붙어야 한다.
`block()`이 throw하고 `resetToPublic()`도 throw하면 original `block()` failure가 primary로 남고,
reset failure는 suppressed로 추가되며, original failure를 다시 던지기 전에 connection을
evict한다. `block()`은 성공했지만 reset이 실패하면 `tenantTransaction`은 isolation 보존을 위해
business work를 rollback하고, connection을 evict하며, tenant/operation context가 담긴 warning을
로그로 남긴 뒤 `TenantSchemaResetFailedException`을 던진다.

Tenant header는 `TenantId` whitelist를 통해서만 mapping된다. Raw request header는 SQL에 절대
concatenate하지 않으며, missing, blank, unknown tenant value는 schema selection 전에 거부한다.
64자를 넘는 tenant header value도 lookup 전에 거부한다. Servlet filter는 downstream handling을
`try/finally`로 감싸고 항상 `TenantContext.clear()`를 호출해야 한다. 이 예제는
servlet-synchronous 전용이며 async servlet dispatch, WebFlux, `@Async`, coroutine context
propagation을 지원하지 않는다.

Tenants:

- `acme` -> `TENANT_ACME`
- `globex` -> `TENANT_GLOBEX`

Sample domain은 의도적으로 작다. SKU를 key로 하는 inventory item만 다룬다. Seed data는
schema마다 달라 test가 각 tenant가 자기 data를 읽는다는 점을 증명할 수 있다.
`ApplicationRunner`는 seed data를 insert하기 전에 두 tenant schema를 idempotent하게 만들어
반복 test/application start가 DDL race를 만들지 않게 한다. Runner는 Spring application startup
중 완료돼야 하며, integration test는 `ApplicationContext`가 ready 상태가 된 뒤에만 실행된다.

Security model: 이 예제는 routing을 보여 주기 위해서만 `X-Tenant-ID`를 신뢰한다. Production
system은 caller-controlled header를 신뢰하지 말고 authenticated session, token, server-side
account mapping에서 tenant를 도출해야 한다. Reset target `PUBLIC`은 H2-specific이다.
PostgreSQL은 `public`과 `search_path` semantics를 사용하므로 production PostgreSQL 구현은 H2
command를 그대로 복사하지 말고 reset strategy를 조정해야 한다. README file은
rollback-on-reset-failure trade-off를 명시해야 한다. 성공한 business write 이후 connection
schema를 안전하게 reset할 수 없다면 예제는 tenant isolation을 보존하기 위해 해당 write를
버린다.

## 검증

- `./gradlew projects`는 `:04-schema-per-tenant-spring-web`를 발견해야 한다.
- `./gradlew :04-schema-per-tenant-spring-web:test`.
- Isolation test는 `maximumPoolSize=1`, `minimumIdle=1`로 Hikari connection reuse를 강제한
  뒤, tenant A work 다음 tenant B work가 tenant A data를 볼 수 없음을 증명해야 한다.
- Isolation assertion은 tenant A가 unique SKU를 insert하고, tenant B가 reused connection에서
  해당 SKU에 대해 404 또는 empty result를 받는 사례를 포함해야 한다.
- Reset-failure test는 failing `PUBLIC` reset을 simulate하고, eviction hook 호출 및 다음 tenant
  request의 isolation 유지 여부를 검증해야 한다.
- Reuse test는 예를 들어 다음 값의 `System.identityHashCode`를 기록해 같은 physical Hikari
  connection이 재사용됨을 assert해야 한다.
  `TransactionManager.current().connection.connection`.
- Eviction test는 그 connection을 Hikari proxy로 취급하고 Hikari pool stat 또는 다음 borrow에서
  바뀐 proxy identity로 pool eviction을 검증해야 하며, original reset failure를 primary
  exception으로 보존해야 한다.
- Reset-failure test는 해당 path가 실행될 때 `block()`-failure plus reset-failure 및
  reset-failure plus Exposed cleanup secondary failure 양쪽의 `addSuppressed` behavior를
  assert해야 한다.
- README scan은 committed PNG Architecture Diagram link와 Mermaid block 없음 상태를 보여야 한다.
- `.github/workflows/examples.yml`은 새 모듈을 build해야 한다.
- Nightly DB shard decision을 기록해야 한다. 이 모듈은 H2-focused이며 새 Testcontainers shard가
  필요하지 않다.
