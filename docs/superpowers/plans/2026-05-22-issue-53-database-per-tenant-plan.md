# Issue 53 Database-per-tenant 계획

## 단계

1. Routing 및 docs requirement를 확인한다.
   - Issue #53과 epic #51을 scope로 사용한다.
   - Controller, domain, README, CI, test pattern에 대해 issue #52 schema-per-tenant module과
     비교한다.
   - Fallback behavior를 명시적으로 유지한다: missing tenant는 400, unknown tenant는 404이며 default
     datasource는 사용하지 않는다.

2. 새 module을 만든다.
   - `10-multi-tenant/05-database-per-tenant-spring-web`를 추가한다.
   - Issue #52 Spring MVC/H2 example과 맞는 Gradle dependency를 추가한다.
   - `app.tenants.acme`와 `app.tenants.globex` H2 JDBC URL을 포함한 application YAML을
     추가한다.

3. Tenant routing을 구현한다.
   - `TenantId`, `TenantContext`, `TenantFilter`를 추가한다.
   - `TenantFilter`를 highest precedence로 등록하고 `finally`에서 `TenantContext`를 clear한다.
   - Tenant datasource property와 `TenantDatabaseRegistry`를 추가한다.
   - Safe Hikari default를 고정하고 H2 URL에는 `DB_CLOSE_DELAY=-1`을 요구한다.
   - 유일한 repository transaction boundary로 `TenantTransaction`을 추가한다.
   - Registry가 known tenant를 검증하고 모든 owned datasource를 닫는지 보장한다.
   - Spring lifecycle hook에서 owned datasource를 닫는다.

4. Inventory API를 구현한다.
   - Table/model/repository/service/controller/error-handler class를 추가한다.
   - Tenant마다 구분되는 inventory row를 seed한다.
   - Seed write 전에 tenant database마다 DDL을 한 번 bootstrap한다.
   - 반복 application context start에서도 seed write를 idempotent하게 유지한다.
   - Repository code는 datasource-agnostic하게 유지한다.

5. 집중 test를 추가한다.
   - Valid tenant는 isolated seed data를 읽는다.
   - 한 tenant의 write는 다른 tenant에서 보이지 않는다.
   - Parallel alternating tenant request는 `TenantContext`를 leak할 수 없다.
   - Missing tenant는 400을 반환한다.
   - Unknown tenant는 404를 반환한다.
   - Error response는 stable `{code, message}` JSON을 사용한다.
   - Registry는 incomplete 또는 unknown tenant configuration을 거부한다.
   - Registry close는 모든 owned Hikari datasource를 닫는다.
   - Failing write는 selected tenant database에서만 rollback된다.
   - DDL bootstrap은 모든 configured database에 inventory table을 만든다.

6. 문서와 diagram을 추가한다.
   - English/Korean module README를 추가한다.
   - Header-based tenant routing은 workshop simplification이며 production system은 tenant identity를
     authentication에 bind해야 함을 문서화한다.
   - Architecture PNG/SVG와 sequence PNG/SVG를 추가한다.
   - `10-multi-tenant/README.md`와 `README.ko.md`를 갱신한다.
   - Root `README.md`와 `README.ko.md`를 갱신한다.

7. Selected examples CI를 연결한다.
   - `.github/workflows/examples.yml` trigger에 module path를 추가한다.
   - Selected example build에 `:05-database-per-tenant-spring-web:build`를 추가한다.

8. 검증하고 review한다.
   - Targeted module tests/build를 실행한다.
   - `actionlint`를 실행한다.
   - README diagram scan과 visual PNG inspection을 실행한다.
   - Step 6-R 6-Tier review와 Claude Code CLI review를 실행한다.

9. 게시한다.
   - Lore protocol로 commit한다.
   - Branch를 push하고 `debop`에게 assign된 `develop` 대상 PR을 만든다.
   - 사용할 수 있으면 `examples`와 `documentation` label을 추가한다.
   - Handoff 전에 CI를 watch하고 failure를 처리한다.

## 수용 기준 매핑

- Tenant-specific datasource/database routing:
  Steps 2-4는 `TenantDatabaseRegistry`와 `TenantTransaction`을 구현한다.
- Tenant isolation과 error test 집중:
  Step 5는 isolation, missing/unknown tenant, no fallback을 다룬다.
- README.md 및 README.ko.md 전략 안내:
  Step 6은 database-per-tenant가 적절한 경우를 문서화한다.
- CI/nightly coverage 결정:
  Spec은 H2 selected-example CI coverage를 기록하고 Step 7은 이를 강제한다.

## 검토 메모

구현을 진행하기 전에 Step 2-R/3-R advisor review가 필요하다. Gate는 최신 normalized `P0=0`,
`P1=0` 상태에서만 통과한다.

초기 advisor artifact:
`.omx/artifacts/claude-issue-53-spec-plan-advisor-stdin-6min-20260522235409.md`.
해당 review는 P0=2/P1=6으로 실패했다. Spec과 plan은 이제 accepted fix를 포함한다: ThreadLocal
cleanup, H2 lifecycle, lifecycle close hook, filter ordering, parallel isolation, rollback, DDL
bootstrap, README auth-warning coverage.
