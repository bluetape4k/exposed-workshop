# Issue #138 BigQuery dry-run 구현 계획

> **Agentic worker용:** REQUIRED SUB-SKILL: 이 계획은 task-by-task로 구현하기 위해 superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans를 사용한다. 단계 추적에는 checkbox(`- [ ]`) syntax를 사용한다.

**목표:** Issue #138을 위한 runnable `13-ecosystem-integrations/01-bigquery-dry-run` workshop
module을 추가한다.

**아키텍처:** Module은 credential-free test-driven workshop example이다. H2-backed SQL generation
database에서 Exposed로 analytical SQL을 만들고, mocked BigQuery REST client에 대해
`BigQueryContext.validateQuery`로 generated SQL을 검증한다. Documentation과 flow diagram은
dry-run validation과 billable query execution의 boundary를 설명한다.

**기술 스택:** Kotlin 2.3, Java 21, Gradle Kotlin DSL, JetBrains Exposed v1.x
from the catalog-backed dependency line,
`bluetape4k-exposed-bigquery`, H2, JUnit 5, MockK, `bluetape4k-assertions`,
CairoSVG, GitHub Actions.

---

## 파일 지도

- 수정: `gradle/libs.versions.toml`
  - Centrally governed artifact를 사용하는 `exposed-bigquery` alias를 추가한다.
- 생성: `13-ecosystem-integrations/01-bigquery-dry-run/build.gradle.kts`
  - Module dependency와 test classpath wiring.
- 생성: `13-ecosystem-integrations/01-bigquery-dry-run/src/main/kotlin/exposed/examples/bigquery/dryrun/BigQueryDryRunWorkshop.kt`
  - Read-model query, default dry-run option, validation call을 만드는 public workshop helper API.
- 생성: `13-ecosystem-integrations/01-bigquery-dry-run/src/test/kotlin/exposed/examples/bigquery/dryrun/BigQueryDryRunWorkshopTest.kt`
  - Generated SQL, dry-run option, success, failure test.
- 생성: `13-ecosystem-integrations/01-bigquery-dry-run/src/test/resources/junit-platform.properties`
- 생성: `13-ecosystem-integrations/01-bigquery-dry-run/src/test/resources/logback-test.xml`
- 생성: `13-ecosystem-integrations/01-bigquery-dry-run/README.md`
- 생성: `13-ecosystem-integrations/01-bigquery-dry-run/README.ko.md`
- 생성: `docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.svg`
- 생성: `docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.png`
- 수정: `13-ecosystem-integrations/README.md`
- 수정: `13-ecosystem-integrations/README.ko.md`
- 수정: `README.md`
- 수정: `README.ko.md`
- 수정: `.github/workflows/examples.yml`
- 생성: `docs/review/2026-06-29-issue-138-bigquery-dry-run-code-review.md`
- 생성: `docs/lessons/2026-06-29-issue-138-bigquery-dry-run.md`

## 작업 1: Catalog 및 module skeleton

complexity: low

Module registration과 dependency governance에는 `$bluetape4k-code-patterns`를 적용한다.

- [ ] 추가: `gradle/libs.versions.toml`의 다른 `exposed-*` alias 옆에
  `exposed-bigquery = { module = "io.github.bluetape4k.exposed:bluetape4k-exposed-bigquery" }`.
- [ ] 생성: `13-ecosystem-integrations/01-bigquery-dry-run/build.gradle.kts`:

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(libs.exposed.bigquery)
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)

    runtimeOnly(libs.h2.v2)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.mockk)
}
```

- [ ] 생성: Deterministic request capture를 위해 parallel execution을 비활성화한
  `src/test/resources/junit-platform.properties`.
- [ ] 생성: Repository의 기존 compact test logging pattern을 사용하는
  `src/test/resources/logback-test.xml`.
- [ ] 실행:

```bash
./gradlew projects --quiet
```

예상: exit 0이고 output이 `:01-bigquery-dry-run`을 포함한다.

## 작업 2: Dry-run query validation용 RED test

complexity: medium

`$bluetape4k-code-patterns`, `ecc-kotlin-exposed`, `ecc-kotlin-testing`을 적용한다. TDD를 따라
`BigQueryDryRunWorkshop.kt`를 추가하기 전에 test를 작성한다.

- [ ] 생성:
  `13-ecosystem-integrations/01-bigquery-dry-run/src/test/kotlin/exposed/examples/bigquery/dryrun/BigQueryDryRunWorkshopTest.kt`.
- [ ] 작성: `BigQueryDryRunWorkshop.kt`의 production API symbol을 기대하는 test:
  `Events`, `buildRegionalRevenueQuery`,
  `defaultDryRunOptions`, and `validateRegionalRevenueDryRun`.
- [ ] 추가: 실제 Google API service chain용 MockK stub:
  `Bigquery`, `Bigquery.Jobs`, and `Bigquery.Jobs.Query`.
- [ ] `Jobs.query(projectId, request)`에 전달되는 실제 `QueryRequest` argument를 capture하고
  `Jobs.Query.execute()`에서 configured `QueryResponse`를 반환한다.
- [ ] Placeholder project ID, dataset ID, location에는 named test constant를 사용하고, 해당
  constant로 `BigQueryContext`를 구성한다.
- [ ] 추가: test `generated query dry run maps query job options without credentials`:
  - Exposed grouped query를 만든다.
  - `BigQueryContext.validateQuery`를 호출한다.
  - Captured request field를 `bluetape4k-assertions`로 assert한다.
- [ ] 추가: test `dry run surfaces BigQuery validation errors without execution`:
  - Mocked response는 error를 포함한다.
  - `validateQuery`는 `BigQueryQueryException`을 던진다.
  - Assertion은 `io.bluetape4k.assertions.assertFailsWith`를 사용한다.
- [ ] 실행:

```bash
./gradlew :01-bigquery-dry-run:test --tests 'exposed.examples.bigquery.dryrun.BigQueryDryRunWorkshopTest' --no-daemon
```

예상 RED: `BigQueryDryRunWorkshop.kt`가 아직 구현되지 않았거나 실제 BigQuery API signature에
import/API 조정이 필요해서 test가 실패한다. Failure는 syntax typo가 아니라 missing implementation
또는 API wiring 때문이어야 한다. Existing API가 production helper gap 없이 test를 완전히 충족해
test가 즉시 통과하면 artificial failure를 강제하지 말고 이를 TDD evidence로 기록한다.

## 작업 3: 기존 BigQuery API wiring으로 GREEN 구현

complexity: medium

`$bluetape4k-code-patterns`와 `ecc-kotlin-exposed`를 적용한다.

- [ ] 생성:
  `13-ecosystem-integrations/01-bigquery-dry-run/src/main/kotlin/exposed/examples/bigquery/dryrun/BigQueryDryRunWorkshop.kt`.
- [ ] 공개 KDoc이 있는 workshop helper를 구현한다:
  - `Events` table with `eventId`, `region`, `eventType`, `revenue`, and
    `occurredAt` columns
  - `buildRegionalRevenueQuery`
  - `defaultDryRunOptions`
  - `validateRegionalRevenueDryRun`
- [ ] 실제 `BigQueryContext`, `BigQueryQueryOptions`, `BigQueryQueryPriority`,
  `BigQueryQueryException` API에 대해 test가 compile되도록 import와 helper code를 고친다.
- [ ] 생성: Test setup에서 H2 SQL-generation database를 deterministic하게 만든다.
  Use `Database.connect("jdbc:h2:mem:bigquery_dry_run;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", driver = "org.h2.Driver")`
  또는 사용할 수 있으면 matching `BigQueryContext` factory를 사용한다. Test마다 mocked BigQuery
  REST client를 다시 만들고 mutable request capture를 test별로 유지하며, Exposed state가 test
  사이에 leak되지 않도록 transaction boundary는 `BigQueryContext.validateQuery`에 맡긴다.
- [ ] `System.getenv`, `System.getProperty`, ADC, service-account file, project secret, endpoint
  override, token, API key를 읽거나 real Google Cloud BigQuery client를 만드는 path를 추가하지
  않는다.
- [ ] 회피: deprecated Exposed imports such as `SqlExpressionBuilder.eq`.
- [ ] Exact full-SQL matching 대신 durable SQL fragment assertion을 사용한다.
- [ ] 실행:

```bash
./gradlew :01-bigquery-dry-run:test --no-daemon
```

예상 GREEN: `:01-bigquery-dry-run`의 모든 test가 통과한다.

## 작업 4: Module README pair와 diagram

complexity: medium

`$bluetape4k-diagram`과 README locale policy를 적용한다.

- [ ] 생성: Module 아래 `README.md`와 `README.ko.md`.
- [ ] 포함: language switches:
  - English file: `English | [한국어](README.ko.md)`
  - Korean file: `[English](README.md) | 한국어`
- [ ] 유지: README locale parity. 두 file은 purpose, dry-run vs execution, credential-free
  command, no-cloud-credential guarantee, tested behavior, diagram reference, real BigQuery
  out-of-scope warning에 대한 matching section을 가져야 한다.
- [ ] 설명:
  - Dry run은 billable query를 실행하지 않고 query를 parse/validate한다.
  - Default test path는 mocked BigQuery REST client를 사용한다.
  - Credential, ADC, project secret, network call은 필요하지 않다.
  - command: `./gradlew :01-bigquery-dry-run:test`
  - Expected result: Command는 H2와 mocked BigQuery REST call만 사용하며
    `GOOGLE_APPLICATION_CREDENTIALS` 없이 통과한다.
- [ ] 생성: English label과 source-backed flow를 가진
  `docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.svg`: Exposed query -> SQL
  generation DB -> BigQuery dry-run request -> mocked REST response -> workshop assertions.
- [ ] embed: 두 README file에 diagram을 넣고 `mocked BigQuery REST response`를 포함한 alt text
  또는 caption을 둔다.
- [ ] PNG render:

```bash
~/.local/bin/cairosvg docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.svg \
  -o docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.png -s 2
```

- [ ] 검증:

```bash
xmllint --noout docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.svg
```

- [ ] Diagram을 수용하기 전에 rendered PNG를 full size로 검사한다.

## 작업 5: Chapter, root README, workflow wiring

complexity: low

Module registration과 workflow rule에는 `$bluetape4k-code-patterns`를 적용한다.

- [ ] 갱신: `13-ecosystem-integrations/README.md`와 `README.ko.md`:
  - #138 status를 `Planned`에서 `Ready`로 변경한다.
  - `01-bigquery-dry-run/README.md`와
    `01-bigquery-dry-run/README.ko.md`
    를 link한다.
  - #139-#145는 planned 상태로 유지한다.
- [ ] 갱신: Chapter 13 아래 root `README.md`와 `README.ko.md`에 첫 runnable child module link를
  포함한다.
- [ ] 추가: Chapter 13 comment 근처 selected Examples workflow Gradle invocation에
  `:01-bigquery-dry-run:build`.
- [ ] 완료: New-module registration audit를 수행하고 review document에 결과를 기록한다:
  - `settings.gradle.kts`: Automatic chapter include가 module을 포함한다.
  - Repo-local module list: root/chapter README pair가 갱신됐다.
  - CI workflow: Broad Gradle test job이 discovered project를 다루므로 child-module path/job 변경은
    필요하지 않다.
  - Nightly workflow: 이 module은 mock-only이고 Examples workflow가 runnable example coverage를
    소유하므로 N/A.
  - Examples workflow: Path filter는 이미 chapter 13을 포함하고 selected build task가 추가된다.
  - Summary `needs`: Examples job graph가 바뀌지 않으면 unchanged.
  - Coverage artifact: 변경 없음. Kover threshold나 Codecov gate를 추가하지 않는다.
- [ ] 실행:

```bash
actionlint .github/workflows/examples.yml
```

예상: exit 0.

## 작업 6: 검증과 review evidence

complexity: medium

`verification-before-completion`, `$bluetape4k-code-patterns`, `$bluetape4k-diagram`을
적용한다.

- [ ] 실행: Module test:

```bash
./gradlew :01-bigquery-dry-run:test --no-daemon
```

- [ ] 실행: Module build:

```bash
./gradlew :01-bigquery-dry-run:build --no-daemon
```

- [ ] 실행: Project discovery:

```bash
./gradlew projects --quiet
```

- [ ] 실행: Workflow lint:

```bash
actionlint .github/workflows/examples.yml
```

- [ ] 실행: Diff whitespace check:

```bash
git diff --check
```

- [ ] 실행: 변경된 README file의 local-link check:

```bash
ruby -e 'files=%w[README.md README.ko.md 13-ecosystem-integrations/README.md 13-ecosystem-integrations/README.ko.md 13-ecosystem-integrations/01-bigquery-dry-run/README.md 13-ecosystem-integrations/01-bigquery-dry-run/README.ko.md]; bad=[]; files.each{|f| text=File.read(f); text.scan(/!?\[[^\]]*\]\(([^)#][^)]*)\)/).flatten.each{|href| next if href =~ %r{\Ahttps?://}; path=href.split("#",2).first; next if path.empty?; target=File.expand_path(path, File.dirname(f)); bad << "#{f} -> #{href}" unless File.exist?(target)}}; abort(bad.join("\n")) unless bad.empty?'
```
- [ ] 실행: 변경된 module/docs의 credential drift scan:

```bash
rg -in "GOOGLE_APPLICATION_CREDENTIALS|application-default|Application Default Credentials|\\bADC\\b|service[-_ ]account|client_secret|password|token|api[_-]?key|project[-_ ]?ids?|projectId|endpoint secret" \
  13-ecosystem-integrations README.md README.ko.md docs/images/readme-diagrams .github/workflows/examples.yml
```

예상: Explanatory policy/warning text와 placeholder dataset/project ID만 나오며 real secret은 없다.

- [ ] 실행: Executable-path real-client scan:

```bash
rg -n "System\\.getenv|System\\.getProperty|GoogleCredentials|BigQueryOptions|ServiceAccount|GOOGLE_CLOUD_PROJECT|GOOGLE_APPLICATION_CREDENTIALS|setApplicationName|new Bigquery|Bigquery\\.Builder" \
  13-ecosystem-integrations/01-bigquery-dry-run/src .github/workflows/examples.yml
```

예상: Real-client construction이 아닌 test helper의 mocked `Bigquery` type usage를 제외하면 match
0개.

- [ ] 작성: `docs/review/2026-06-29-issue-138-bigquery-dry-run-code-review.md`
  에 실제 review evidence에서 파생된 severity count와 finding을 기록한다. 검토 완료 전에 P0/P1
  result를 미리 선언하지 않는다.
- [ ] 기록: Generated image를 검사한 뒤 review document에 rendered PNG visual QA evidence를
  기록한다.

## 작업 7: Lesson, commit, PR, CI

complexity: low

- [ ] 생성: Module, credential-free default, workflow task wiring lesson을 담은
  `docs/lessons/2026-06-29-issue-138-bigquery-dry-run.md`.
- [ ] commit: 아직 commit하지 않았다면 implementation 전에 spec과 plan을 commit한다.
- [ ] commit: Implementation/docs/review/lesson을 Lore commit trailer와 함께 commit한다.
- [ ] push: Branch `feat/issue-138-bigquery-dry-run`.
- [ ] 읽기: PR 생성 전 live issue metadata:

```bash
gh issue view 138 --json assignees,labels,milestone,state
```

- [ ] 생성: PR:
  - title: `feat: add BigQuery dry-run workshop example`
  - Body는 #138을 close하고 #137을 reference한다.
  - Assignee, milestone, label은 live issue #138 metadata를 반영한다.
  - 마지막 section은 정확히 `## DoD Status`다.
- [ ] 검증: live PR metadata:

```bash
gh pr view <number> --json body,assignees,labels,milestone,state,isDraft
```

- [ ] 검증: parent epic #137 remains open:

```bash
gh issue view 137 --json state
```

- [ ] `gh pr checks <number>`로 CI를 watch/check하고, check success 또는 evidence-backed blocker를
  보고한 뒤에만 final DoD로 진행한다.

## 단계 3-R Self review

- Spec coverage: 모든 #138 acceptance criterion은 작업 1-7에 mapping된다.
- Placeholder scan: `TBD`, `TODO`, open-ended implementation step이 남아 있지 않다.
- Type consistency: Module name, package name, test class, diagram filename, Gradle task name이
  task 전체에서 일관된다.
- Concurrency helpers: 해당 없음. 이 module에는 race, structured concurrency, virtual-thread,
  suspend stress behavior가 없다.
- Testcontainers: 해당 없음. Default path는 mock REST client와 H2 SQL generation DB다.
