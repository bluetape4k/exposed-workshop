# Issue 53 Database-per-tenant 설계

## 배경

Issue #53은 issue #52의 schema-per-tenant 예제 이후 10장 Spring Boot multi-tenant strategy
set을 확장한다. 목표는 datasource/database boundary에서 tenant routing을 가르치는 것이다. 즉
허용된 각 tenant는 자체 datasource와 Exposed `Database`로 mapping되고, 모든 repository
transaction은 선택된 tenant database에서 실행된다.

이는 새 example module, public README material, CI wiring, 향후 예제가 비교할 routing
boundary를 추가하므로 Type A Full Design change다.

## 범위

- `10-multi-tenant/05-database-per-tenant-spring-web`를 추가한다.
- 두 H2 in-memory tenant database와 함께 Spring MVC 및 Exposed JDBC를 사용한다:
  `acme`와 `globex`.
- Strict whitelist를 통해 `X-Tenant-ID`에서 tenant를 resolve한다.
- Exposed transaction은 `TenantDatabaseRegistry`를 통해 route한다.
- 각 tenant database를 독립적으로 seed한다.
- Tenant isolation, missing tenant, unknown tenant, no-default fallback, datasource lifecycle
  assumption을 테스트한다.
- Committed architecture diagram PNG와 추가 sequence diagram PNG가 있는 English/Korean README를
  추가한다.
- 모듈을 Chapter 10 docs, root README file, selected examples CI에 연결한다.

범위 제외:

- Runtime tenant provisioning. Issue #55가 onboarding/provisioning을 소유한다.
- Spring Security-bound tenant authorization. Issue #54가 auth coupling을 소유한다.
- 이 모듈의 container-backed database matrix coverage. 예제는 PR CI를 빠르게 유지하기 위해 H2를
  사용하며, CI decision은 README와 lesson에 문서화한다.

## 설계

### Tenant resolution

`TenantFilter`는 `X-Tenant-ID`를 읽고 trim/lowercase로 normalize한 뒤 `TenantId`에 mapping한다.
허용 값은 `acme`와 `globex`다.

Missing tenant는 caller error이며 HTTP 400을 반환한다. Unknown tenant는 default database로
조용히 mapping하지 않고 HTTP 404를 반환한다. Default datasource는 isolation misconfiguration을
숨기고 tenant data를 leak할 수 있으므로 fallback routing을 의도적으로 거부한다.

`TenantContext`는 Spring MVC chapter pattern 및 issue #52 module과 맞게 `ThreadLocal`을 통해
request scoped 상태로 유지된다.

`TenantFilter`는 controller/repository exception path를 포함해 항상 `finally`에서
`TenantContext`를 clear해야 한다. Filter는 highest precedence로 등록되어 모든 downstream MVC
handler와 error handler가 resolved tenant를 볼 수 있고, 이후 request가 stale `ThreadLocal`
state를 상속하지 못하게 한다.

이 예제는 의도적으로 tenant authorization을 수행하지 않는다. README는 `X-Tenant-ID`를 workshop
routing 용도로만 신뢰한다고 경고해야 한다. Production system은 tenant를 authenticated claim
또는 server-side session state에 bind해야 한다. Issue #54가 해당 security-bound 예제를
소유한다.

### Database registry

`TenantDataSourceProperties`는 `app.tenants` 아래 per-tenant Hikari setting을 정의한다.
`DatabaseConfiguration`은 configured `TenantId`마다 하나의 `HikariDataSource`와 하나의 Exposed
`Database`를 만든다. 각 tenant database가 application context 생명주기 동안 connection churn을
견디도록 H2 URL에는 `DB_CLOSE_DELAY=-1`이 포함돼야 한다. YAML이 non-safety field를 override하지
않는 한 Hikari default는 code에 고정한다: maximum pool size 4, minimum idle 1, connection
timeout 5 seconds, pool name `tenant-{id}`.

`TenantDatabaseRegistry` exposes:

- `databaseFor(tenantId: TenantId): Database`
- `dataSourceFor(tenantId: TenantId): DataSource`
- `configuredTenants(): Set<TenantId>`
- lifecycle cleanup을 위한 `close()`

Registry는 bean initialization 시 모든 `TenantId`에 datasource가 있고 unknown tenant key가
configuration에 없음을 검증한다. Unknown 또는 incomplete tenant configuration은 첫 request가
아니라 startup을 실패시킨다. Registry는 datasource lifecycle을 소유하는 유일한 위치이며
`@PreDestroy`/`DisposableBean` hook으로 모든 owned `HikariDataSource` instance를 닫는다.

### Transaction boundary

`TenantTransaction.execute { ... }`는 `TenantContext`에서 current `TenantId`를 얻고 해당 Exposed
`Database`를 resolve한 뒤 `transaction(db)`를 실행한다.

Repository는 datasource를 직접 선택하면 안 된다. Repository는 `TenantTransaction`에 의존해
tenant/database routing boundary를 명시적이고 감사 가능하게 만든다.

Rollback behavior는 Exposed transaction이 소유한다. Test는 실패한 tenant write가 해당 tenant
database에서만 rollback되고 다른 tenant database를 변경하지 않음을 증명해야 한다.

### Domain

새 모듈은 issue #52의 inventory domain shape를 재사용한다.

- `InventoryItems` table
- `InventoryItemRecord`
- `CreateInventoryItemRequest`
- `InventoryRepository`
- `InventoryService`
- `InventoryController`

Table name은 모든 tenant database에서 같지만, 각 tenant가 다른 JDBC URL과 Exposed `Database`를
갖기 때문에 physical storage는 분리된다.

### Seeding

`InventorySeeder`는 configured tenant를 순회하고 `InventoryItems`를 만든 뒤 request가 사용하는
동일한 repository/transaction path로 tenant-specific row를 seed한다. DDL bootstrap은 seed write
전에 tenant database마다 한 번씩 실행돼야 한다. Seed
operation은 default row insert 전에 tenant row 존재 여부를 확인해 반복 context start에서도
idempotent하다. Test는 `acme`와 `globex`가 서로 다른 seed data를 본다고 assert한다.

### Error contract

- Missing `X-Tenant-ID`: `MISSING_TENANT`와 함께 400.
- Unknown `X-Tenant-ID`: `UNKNOWN_TENANT`와 함께 404.
- Known tenant의 datasource가 registry에 없으면 startup failure.
- Default tenant/database fallback은 허용하지 않는다.

Error response는 stable JSON shape를 사용한다.

```json
{"code":"MISSING_TENANT","message":"X-Tenant-ID header is required"}
```

### README diagram

두 README file은 `docs/images/readme-diagrams/` 아래 같은 committed PNG file을 link한다.

- `10-multi-tenant-05-database-per-tenant-spring-web-architecture-01.png`
- `10-multi-tenant-05-database-per-tenant-spring-web-sequence-02.png`

SVG source는 PNG 옆에 commit한다. Diagram text는 English다.

## 검증

Local verification:

- `./gradlew :05-database-per-tenant-spring-web:test --stacktrace --continue`
- `./gradlew :05-database-per-tenant-spring-web:build --stacktrace --continue`
- `./gradlew projects --quiet | rg '05-database-per-tenant-spring-web'`
- `actionlint .github/workflows/examples.yml`
- Architecture Diagram PNG link와 Mermaid block 없음 상태를 확인하는 README diagram scan.
- Generated PNG의 readable contrast에 대한 visual inspection.

Required test coverage:

- MVC thread pool을 통한 parallel alternating request는 `TenantContext`를 leak할 수 없다.
- 실패한 tenant write는 selected tenant에서만 rollback된다.
- DDL bootstrap은 configured tenant database마다 `InventoryItems`를 만든다.
- Unknown tenant config key와 missing known tenant config는 fail fast한다.
- Registry close는 모든 owned Hikari datasource를 닫는다.

Review gate:

- 이 spec/plan에 대해 stdin과 timeout >= 5 minutes를 사용하는 Claude Code CLI로 Step 2-R/3-R
  advisor review를 수행한다.
- 구현 후 6-Tier frame과 stdin 및 timeout >= 5 minutes를 사용하는 Claude Code CLI로 Step 6-R
  code review를 수행한다.

## CI 결정

이 모듈은 H2-only tenant database를 사용하고 Testcontainers를 시작하지 않으므로 selected
examples CI에서 실행해야 한다. Non-doc file이 변경되면 full repository DB matrix coverage는
기존 CI workflow로 계속 실행되지만, 모듈 자체는 빠르고 deterministic하게 유지되도록 설계됐다.

## Advisor gate

- Initial artifact:
  `.omx/artifacts/claude-issue-53-spec-plan-advisor-stdin-6min-20260522235409.md`
- Result: FAIL, P0=2, P1=6.
- 반영한 수정: ThreadLocal cleanup/filter ordering, H2 URL lifecycle, README auth-trust warning,
  registry lifecycle hook, concurrent isolation test, rollback test, DDL bootstrap, stable error
  JSON, fail-fast configuration validation.
