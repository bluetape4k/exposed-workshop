# Issue 52 Schema-per-tenant 계획

## 단계

1. Spring Boot module skeleton과 dependency를 추가한다.
2. Tenant resolution, `tenantTransaction { }`, Exposed repository, service, controller, seed
   initializer를 구현한다. `tenantTransaction { }`은 `SchemaUtils.setSchema(Schema(...))`를
   사용해야 하며, raw tenant string을 SQL에 concatenate하면 안 된다. Transaction 시작 시 tenant
   schema를 설정하고 반환 전에 `PUBLIC`으로 reset해야 한다. Reset이 실패하면 original reset
   failure를 다시 던지기 전에 Hikari connection eviction hook을 호출해야 한다. Reset-failure 및
   same-connection reuse test를 위해 `SchemaResetter`, `ConnectionEvictor`, connection-probe seam을
   추가한다. Eviction target은 unwrapped driver connection이 아니라
   `TransactionManager.current().connection.connection`에서 얻은 Hikari-borrowed proxy connection이다.
3. Request error, valid routing, cross-tenant isolation을 위한 집중 Spring Boot test를 추가한다.
   Tenant request 간 connection reuse를 강제하도록 test datasource는 작은 Hikari pool
   (`maximumPoolSize=1`, `minimumIdle=1`)로 설정한다.
4. English/Korean README file과 committed architecture diagram PNG/SVG를 추가한다.
5. Module을 Examples workflow에 연결하고 필요하면 chapter/root docs를 갱신한다.
6. PR을 열기 전에 local verification과 6-Tier review를 실행한다.

## 수용 기준 매핑

- 독립 build: targeted Gradle test/build task.
- Tenant isolation: 같은 SKU가 tenant마다 다른 값으로 존재할 수 있고, 새 tenant-local SKU는 다른
  schema에서 보이지 않는다.
- Error handling: missing/unknown tenant header는 HTTP 400을 반환한다.
- Context safety: downstream request handling이 실패해도 `TenantContext`는 항상 clear된다.
- Reset safety: reused connection에서 tenant A request 다음 tenant B request가 실행돼도 tenant B는
  tenant A data를 읽을 수 없고, tenant transaction 이후 current schema는 `PUBLIC`으로 reset된다.
- Reset failure: test-only schema reset failure는 eviction hook을 호출하고 다음 tenant request의
  isolation은 유지된다. Exposed가 secondary rollback/close failure를 발생시켜도 original reset
  exception은 primary로 남는다.
- Successful business block 이후 reset failure는 `TenantSchemaResetFailedException`을 던지고
  business write를 rollback하며 tenant/operation context를 warn level로 log하고 connection을
  evict한다.
- Business block failure 이후 reset failure는 business exception을 primary로 보존하고 reset failure를
  `addSuppressed`로 붙인다.
- Same-connection proof: pool=1 isolation test는 physical connection identity를 기록하고 tenant A/B
  operation이 reused connection을 사용했음을 검증한 뒤 B가 A-only data를 읽을 수 없음을 assert한다.
- Pool-state proof: reset-failure test는 Hikari pool active connection이 0으로 돌아오고 다음 tenant
  request에 fresh borrow가 사용됨을 assert한다.
- Failure cleanup: tenant resolution 이후 controller exception이 발생해도 `TenantContext`가 clear된다.
  다음 missing-tenant request는 stale tenant를 사용하지 않고 거부된다.
- Header hardening: blank, uppercase, path-like, SQL-like, oversized tenant value를 거부한다.
- Documentation: README pair는 schema-per-tenant가 적합한 경우와 database-per-tenant가 더 나은 다음
  단계인 경우를 설명한다.
