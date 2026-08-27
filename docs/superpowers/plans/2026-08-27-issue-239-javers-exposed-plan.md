# JaVers + Exposed Audit Workshop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exposed DAO 변경을 JaVers 0.3.0 JDBC 저장소에 기록하고 actor/requestId, diff, 이력, rollback 경계를 H2 테스트와 source-equivalent 문서로 증명한다.

**Architecture:** `CustomerEntity`는 업무 원장만 관리하고 `AuditedCustomer` detached DTO를 `ExposedJaversEntityHookMapping`으로 감사한다. `AuditContextHolder`가 동기 JDBC 트랜잭션의 actor/requestId를 제공하며 `JaversAuditHistory`가 JaVers JQL 조회를 감싼다.

**Tech Stack:** Kotlin language/API 2.3 (catalog/plugin 2.4.0), Java 25, Exposed 1.4 JDBC DAO, `bluetape4k-javers-exposed:0.3.0` (central BOM 관리), H2 2.4, JUnit 5, Kluent-style `bluetape4k-junit5` assertions, CairoSVG.

---

## 1. 의존성·모듈 골격을 만든다

- [ ] `gradle/libs.versions.toml`의 `# bluetape4k` 영역에
  `bluetape4k-javers-exposed = { module = "io.github.bluetape4k.javers:javers-exposed" }`
  alias를 추가한다. `bluetape4k-dependencies:1.4.0` BOM이 provider 버전
  `0.3.0`을 관리하도록 별도 version key는 추가하지 않는다.
- [ ] `13-ecosystem-integrations/12-javers-exposed-audit/build.gradle.kts`를
  추가한다.
  - `implementation(libs.bluetape4k.javers.exposed)`
  - `implementation(libs.jetbrains.exposed.core)`와 `libs.jetbrains.exposed.dao`,
    `libs.jetbrains.exposed.jdbc`
  - `runtimeOnly(libs.h2.v2)`
  - `testImplementation(libs.bluetape4k.junit5)`
  - 기존 chapter 13 모듈과 동일한 `testImplementation` configuration 상속을
    사용한다.
- [ ] `src/test/resources/junit-platform.properties`와 `logback-test.xml`을
  기존 결정론적 chapter 13 모듈 형식으로 추가한다.
- [ ] `./gradlew projects --no-daemon --no-configuration-cache`에서
  `:12-javers-exposed-audit`가 자동 include 되는지 확인한다.

## 2. 실패하는 계약 테스트를 먼저 작성한다

- [ ] `src/test/kotlin/exposed/examples/javers/audit/JaversExposedAuditWorkshopTest.kt`
  를 만들고 테스트마다 고유한 H2 `Database.connect`를
  `MODE=PostgreSQL;DB_CLOSE_DELAY=-1`로 생성한다. 테스트 인스턴스는
  `@TestInstance(PER_CLASS)`/`@Execution(SAME_THREAD)`로 실행하지만
  `@BeforeEach`에서 새 database·repository·JaVers fixture를 만들고,
  `@AfterEach`에서 남은 subscription/context를 정리한다. provider의 head
  캐시와 schema drop/create가 테스트 사이에 공유되지 않도록 repository/Javers
  재사용을 금지한다.
- [ ] `@BeforeEach`에서 `Customers`, `CdoSnapshotTable`, `CommitTable`을
  drop/create하고 `ExposedCdoSnapshotRepository(database).ensureSchema()`가
  반복 호출되어도 성공하는지 첫 테스트로 고정한다.
- [ ] 다음 테스트를 구현 계약으로 먼저 추가한다.
  1. 생성 후 수정하면 감사 기준 데이터 2건, 두 commit metadata의 `author`,
     `requestId`, `changeType`, update diff 및 최신 `name`/`email`을 조회한다.
  2. 한 transaction 안에서 같은 entity를 여러 번 수정하면 최종 값과 감사
     기준 데이터 2건(Initial + Update)을 확인한다.
  3. `AuditContextHolder.with` 내부 transaction에서 예외를 던지면 업무 행,
     `javers_commit`, `javers_snapshot` 행이 모두 0건임을 확인한다.
  4. 이전 값과 같은 값을 별도 transaction에서 다시 저장해도 새로운 감사
     기준 데이터와 성공 이력이 추가되지 않음을 확인한다.
  5. 생성과 `secret`만 변경한 update 뒤 `CdoSnapshotTable.state`와
     `changedProperties`의 모든 저장 행을 직접 읽어 `secret` property/key가
     없음을 확인한다.
  6. subscription `close()` 뒤 생성한 entity가 감사 기준 데이터를 추가하지
     않으며, `close()`를 두 번 호출해도 안전한지 확인한다.
  7. 중첩 `AuditContextHolder.with`가 바깥 context를 복원하고, 최상위
     block의 정상·예외 종료 뒤 `requireCurrent()`가 실패하는지 확인한다.
  8. 문맥 없이 DAO를 변경하면 `IllegalStateException`이 발생하고
     `Customers`, `javers_commit`, `javers_snapshot` 행이 모두 0건인 것을
     확인하여 fail-closed/원자적 rollback을 증명한다.
- [ ] 테스트 단언은 `CdoSnapshot.getPropertyValue`, `commitMetadata.author`,
  `commitMetadata.properties`, `changes`의 실제 관찰값만 사용하고 전체
  pretty-print 문자열에 의존하지 않는다.

## 3. 업무 모델·문맥·조회 API를 구현한다

- [ ] `src/main/kotlin/exposed/examples/javers/audit/JaversExposedAuditWorkshop.kt`
  에 `Customers : IntIdTable("audit_customers")`를 정의한다. `name`, `email`,
  `secret` 컬럼은 길이 제한을 명시하고, 민감 필드는 감사 DTO에 넣지 않는다.
- [ ] `CustomerEntity : IntEntity`와 `IntEntityClass`를 정의한다. `toAuditObject()`
  는 `AuditedCustomer(@Id id, name, email)`만 생성하며 entity 자체를 반환하지
  않는다.
- [ ] 공개 table/entity/context/history/factory API에는 한국어 KDoc을 작성하고,
  `secret` allow-list, 동기 JDBC `ThreadLocal` 범위, subscription `close()` 계약,
  자동 복원을 하지 않는 조회 전용 경계를 KDoc에 명시한다.
- [ ] `AuditContext(actor, requestId)` 생성 시 `requireNotBlank`를 적용하고,
  `AuditContextHolder.with`는 이전 context를 `try/finally`에서 복원하거나
  제거한다. `requireCurrent()`는 context 부재를 명시적 오류로 알린다.
- [ ] `JaversAuditHistory`를 정의한다.
  - `snapshots(customerId)`는 `QueryBuilder.byInstanceId`로
    `javers.findSnapshots`를 호출한다.
  - `changes(customerId)`는 동일 JQL로 `javers.findChanges`를 호출한다.
  - `history(customerId)`는 두 결과와 최신 상태를 한 immutable 결과 객체로
    묶되 원장 변경/자동 복원을 수행하지 않는다.
- [ ] `snapshots`/`changes`/`history` KDoc과 README에 현재 파사드는 교육용
  무제한 이력 조회이며 production pagination·retention 정책을 제공하지
  않는다고 명시한다.
- [ ] `createJavers(database)` 또는 동등한 팩토리에서
  `ExposedCdoSnapshotRepository(database).ensureSchema()` 후
  `JaversBuilder.javers().registerJaversRepository(repository).build()`를
  반환한다.
- [ ] `subscribeAudit(database, javers)`에서
  `ExposedJaversEntityHookMapping.of(CustomerEntity) { it.toAuditObject() }`와
  `ExposedJaversEntityHookSubscription.subscribe`를 연결한다.
  - 전역 hook의 이벤트 database가 전달한 `database`와 다르면 fail-closed하고,
    다른 database에 업무·감사 행을 남기지 않는 부정 테스트를 추가한다.
  - 전역 hook 중복 구독은 명시적으로 거부하고, `close()` 뒤 재구독이 가능한지
    lifecycle 테스트로 고정한다.
  - `authorProvider`는 현재 actor를 반환한다.
  - `commitPropertiesProvider`는 requestId와 `change.changeType.name`만
    반환한다.
  - subscription을 `AutoCloseable` 반환값으로 노출해 호출자가 반드시 닫게
    한다.
  - README와 테스트 fixture는 `try/finally` 또는 `.use` scoped lifecycle을
    사용하고, provider가 전역 hook을 등록하므로 동시 `close()` 조정은 지원
    범위 밖임과 database별 단일 subscription 계약을 명시한다.

## 4. 계약 테스트를 통과시키고 API 경계를 다듬는다

- [ ] 구현 전에 `docs/review/2026-08-27-issue-239-javers-exposed-risk-prediction.md`
  의 provider resolution, transaction atomicity, global hook/context lifecycle,
  sensitive payload, schema fixture, docs/CI 누락 위험을 확인한다. 각 위험의
  signal·mitigation·rollback/rerun 지점을 먼저 기록하고, benchmark·remote DB·
  coroutine은 승인된 one-developer JDBC 범위의 N/A 근거로 남긴다.
- [ ] `./gradlew :12-javers-exposed-audit:test --no-daemon --no-configuration-cache`
  로 실패 원인을 읽고, provider API와 Exposed v1 import를 실제 컴파일
  시그니처에 맞춘다.
- [ ] 생성·수정·rollback·민감 필드·lifecycle 테스트가 모두 통과할 때까지
  한 번에 한 계약만 수정한다. 테스트가 전역 hook 오염으로 흔들리면 fixture의
  `try/finally { subscription.close() }`와 schema 정리를 먼저 점검한다.
- [ ] `./gradlew :12-javers-exposed-audit:detekt
  :12-javers-exposed-audit:build --no-daemon --no-configuration-cache`를
  실행하고 경고/format 문제를 실제 코드에서 해결한다.
- [ ] `./gradlew :12-javers-exposed-audit:koverXmlReport --no-daemon
  --no-configuration-cache`를 실행하고 XML 산출물의 존재를 확인한다.

## 5. README와 다이어그램을 작성한다

- [ ] 모듈 `README.md`와 `README.ko.md`를 source-equivalent로 작성한다.
  - provider 버전과 BOM 경로, 공개 Kotlin 예제, actor/requestId, 조회/diff,
    rollback 의미, `secret` 제외 정책을 설명한다.
  - `actor`는 실제 서비스에서 인증된 caller identity로 공급해야 하며, 이
    교육 예제는 인증/인가를 구현하지 않는다고 명시한다.
  - `history(customerId)`에는 인증·인가·tenant filter가 없고, raw `javers.commit`은
    detached allow-list와 `secret` 제외를 우회할 수 있으므로 production endpoint와
    직접 commit에 사용하지 않는다는 경고를 EN/KO에 동등하게 넣는다.
  - `secret`은 교육용 가짜 평문 필드이며 실제 credential은 암호화·별도 보호가
    필요하다는 경계를 명시한다.
  - `./gradlew :12-javers-exposed-audit:test`와 build 명령을 제공한다.
  - H2 결정론적 범위와 Docker/자격 증명/원격 DB 미사용을 명시한다.
  - `ensureSchema()`는 H2 교육용 편의 기능이고 실제 운영 migration 소유자는
    외부이며, provider의 `createSchemaOnEnsure=false` 선택지를 설명한다.
  - 전체 이력을 조회하는 API는 운영 pagination/retention을 대체하지 않는다고
    명시한다.
  - R2DBC는 `exposed-r2dbc-workshop` 범위임을 명시하고 이 저장소에서 구현하지
    않는다.
- [ ] 저장소 루트 `docs/images/readme-diagrams/`에 다음 편집 가능한 SVG와
  동일 렌더링 PNG를 추가한다.
  - `13-javers-exposed-architecture-01.svg/.png` 및 `.ko.svg/.ko.png`
  - `13-javers-exposed-sequence-01.svg/.png` 및 `.ko.svg/.ko.png`
  - `13-javers-exposed-erd-01.svg/.png` 및 `.ko.svg/.ko.png`
- [ ] SVG는 아키텍처의 caller→DAO→EntityHook→JaVers→Exposed tables,
  sequence의 commit/rollback/close 분기, ERD의 `customers`와
  `javers_commit`/`javers_snapshot` 관계를 실제 코드 이름과 일치시킨다.
- [ ] CairoSVG로 SVG를 PNG로 렌더링하고 XML/text/connector/arrowhead/geometry,
  PNG asset-pair 및 full-size 시각 검사를 실행한다. README에는 raw Mermaid를
  남기지 않는다.

## 6. CI와 chapter index를 연결한다

- [ ] `.github/scripts/select-changed-examples.sh`의 `ALL_TASKS`에
  `:12-javers-exposed-audit:build`를 추가한다. 기존 generic chapter 13 path
  mapping이 새 모듈을 선택하는지 diff-range 명령으로 확인한다.
- [ ] `13-ecosystem-integrations/README.md`와 `.ko.md`에 #239 모듈 행을
  추가한다. 실제 모듈/README/다이어그램 링크를 사용하고, 문서-only 행을
  먼저 만들지 않는다.
- [ ] Nightly matrix에는 추가하지 않는다. 이 모듈은 H2 단일 JVM 예제이고
  Docker/Testcontainers 또는 원격 서비스 계약이 없으며 weekly Examples가
  변경 path를 이미 포괄한다는 근거를 verification 문서에 남긴다. 동시에
  Nightly H2 전체 `test` 및 root CI 전체 테스트에 새 project가 암묵적으로
  포함되는지 workflow line과 실행 결과로 증명한다.
- [ ] `dependencyInsight`로 `io.github.bluetape4k.javers:javers-exposed:0.3.0`
  이 central BOM에서 선택되는지 확인하고, `.github/workflows/examples.yml`
  에 대해 `actionlint`와 실제 diff-range selector 출력을 기록한다.

## 7. 증적·리뷰·PR 전 검증을 남긴다

- [ ] `docs/review/2026-08-27-issue-239-javers-exposed-verification.md`에
  기준 commit SHA, 실행 명령, 성공/실패/skip 수, CI lane 결정, 남은 위험을
  한국어로 기록한다.
- [ ] `git diff --check`, Gradle test/detekt/build, module discovery,
  examples selector, README locale parity, diagram asset-pair/audit를 모두
  fresh head에서 다시 실행한다.
- [ ] 다음 통합 검증 명령과 산출물을 증적에 포함한다.
  ```bash
  ./gradlew :12-javers-exposed-audit:dependencyInsight \
    --dependency javers-exposed --configuration testRuntimeClasspath \
    --no-daemon --no-configuration-cache
  ./gradlew :12-javers-exposed-audit:koverXmlReport \
    --no-daemon --no-configuration-cache
  actionlint .github/workflows/examples.yml
  FORCE_ALL=false .github/scripts/select-changed-examples.sh \
    "$(git rev-parse HEAD^)...$(git rev-parse HEAD)"
  ```
  실제 Kover XML 경로 존재 검사는 모듈의 Gradle 산출물 경로를
  `find 13-ecosystem-integrations/12-javers-exposed-audit/build -name '*.xml'`
  로 확인한다.
- [ ] Type A 독립 리뷰 관점(아키텍처, 보안, 성능/안정성, 사용자 API, 테스트,
  구현)을 읽기 전용으로 수집하고, P0/P1/P2와 수정 여부를 리뷰 문서에
  기록한다. 1인 개발자 범위에서 인간 리뷰가 불가능하면 그 사실과 대체
  검증 근거를 명시한다.
- [ ] `gh issue view 239`와 변경 PR metadata를 live read-back하여 Korean
  title/body, assignee `debop`, milestone `1.4.0`, labels, issue/PR link,
  DoD 체크리스트가 일치하는지 확인한다.

## 8. 커밋·PR·merge 준비

- [ ] 모든 커밋은 Lore intent line과 `Constraint`, `Rejected`, `Confidence`,
  `Scope-risk`, `Directive`, `Tested`, `Not-tested` trailer를 포함한다.
- [ ] 구현/문서/리뷰 증적을 작은 논리적 커밋으로 정리하고, PR 본문은 한국어로
  이슈 링크, 문제/해결, 범위 밖(R2DBC), 테스트, CI, 리뷰, DoD를 구조화한다.
- [ ] PR 생성 후 exact head SHA의 CI와 PR body/read-back을 확인한다. merge는
  여기서 멈추며, 정확한 live head에 대한 사용자의 신선한 `승인` 없이는
  merge/branch deletion/로컬 정리를 수행하지 않는다.
