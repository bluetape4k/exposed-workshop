# Issue #138 BigQuery dry-run workshop 설계

## 배경

Issue #138은 13장 `13-ecosystem-integrations` 아래 첫 runnable child example이다. Parent epic
#137은 Exposed 1.11 ecosystem example을 위한 chapter boundary를 만들고 이 모듈을 위해
`13-ecosystem-integrations/01-bigquery-dry-run`을 예약했다.

`bluetape4k-exposed` issue #228 already added the product feature:
`BigQueryContext`, `BigQueryQueryOptions`, dry-run validation for raw/generated
SQL, and query-job options such as billed-byte limits, labels, priority,
location, destination table, timeout, query cache flag. 해당 repository에는 compact
`examples-exposed-bigquery-dry-run` example도 있다. 이 workshop module은 그 예제를 단순 복사하면
안 된다. Exposed analytical read model이 generated SQL이 되고 다시 credential-free BigQuery
dry-run request가 되는 흐름을 보여 줘 `exposed-workshop` reader의 learning path를 더 명확히
해야 한다.

## 목표

- 첫 runnable chapter 13 module을 추가한다:
  `13-ecosystem-integrations/01-bigquery-dry-run`.
- Raw SQL validation뿐 아니라 `BigQueryContext.validateQuery`를 통한 generated SQL validation을
  보여 준다.
- Mocked BigQuery REST client를 사용해 default path를 credential-free, deterministic,
  network-free, cost-free 상태로 유지한다.
- Test에서 generated SQL, query-job option mapping, success handling, failure handling을
  검증한다.
- Dry-run validation과 query execution의 차이를 명확히 설명하는 English/Korean module README를
  추가한다.
- README diagram을 editable SVG와 rendered PNG로 추가한다.
- Chapter README pair에서 issue #138을 `Planned`에서 `Ready`로 승격하고 weekly Examples
  workflow에 module task를 추가한다.

## 비목표

- Default test에서 real Google Cloud BigQuery에 접속하지 않는다.
- Application Default Credentials, local credential file, service account, project secret,
  user-provided cloud endpoint를 default로 사용하지 않는다.
- 이 예제에 BigQuery emulator 또는 Testcontainers dependency를 추가하지 않는다. 수용 기준은
  dry-run request construction과 error mapping이며, mocked REST client로 deterministic하게
  테스트할 수 있다.
- #139-#145를 구현하지 않는다.
- Parent epic #137을 닫지 않는다.

## 현재 근거

- `settings.gradle.kts`는 이미 `includeModules("13-ecosystem-integrations", false, false)`로
  `13-ecosystem-integrations`를 scan하므로, `build.gradle.kts`가 있는 child directory를 추가하면
  Gradle project `:01-bigquery-dry-run`이 만들어진다.
- `.github/workflows/examples.yml`에는 이미 chapter 13 path filter가 있지만 chapter 13 Gradle
  task는 없다. Child module PR은 selected Examples build list에 `:01-bigquery-dry-run:build`를
  추가해야 한다.
- `.github/workflows/ci.yml`과 `.github/workflows/nightly.yml`은 이미 broad `test` 또는 matrix
  coverage task를 실행하며, 이 mock-only module에는 child-module-specific path addition이
  필요하지 않다. 구현은 registration audit 중 CI, Nightly, summary `needs`, coverage artifact
  change에 대한 명시적 N/A evidence를 그래도 기록해야 한다.
- `gradle/libs.versions.toml`은 `bluetape4k-dependencies` 1.3.1을 import하고 common bluetape4k
  및 Exposed module alias를 이미 갖고 있지만, 아직 `bluetape4k-exposed-bigquery`를 노출하지
  않는다.
- `bluetape4k-dependencies/gradle/libs.versions.toml`은 `bluetape4k-exposed-bigquery`를
  정의하므로 이 repo도 extra local version을 pin하지 않고 같은 catalog alias를 추가할 수 있다.
- `bluetape4k-exposed`에는 다음 source API가 있다:
  `BigQueryContext`, `BigQueryQueryOptions`, `BigQueryQueryPriority`,
  `BigQueryQueryException`, and `BigQueryResultRow`.
- Sibling example은 `MockK`로 `QueryRequest`를 capture하며, 이는 이 workshop module에 맞는
  credential-free testing strategy다.

## 설계

Potentially billable warehouse query가 실행되기 전에 dashboard read model을 검증하는 하나의
scenario에 집중한 작은 모듈을 만든다.

모듈은 다음을 포함한다.

- `build.gradle.kts`
  - `libs.exposed.bigquery`, JetBrains Exposed core/JDBC, H2, MockK, `bluetape4k-junit5`에
    의존한다.
  - Dependency version은 imported bluetape4k BOM이 관리하게 유지한다.
- `README.md`와 `README.ko.md`
  - dry run vs execution을 설명한다.
  - credential-free default command를 문서화한다.
  - test가 무엇을 검증하는지 설명한다.
  - real-service execution이 의도적으로 이 issue scope 밖임을 경고한다.
- one small production source package:
  `exposed.examples.bigquery.dryrun`
  - `BigQueryDryRunWorkshop.kt`
  - generated SQL을 위한 local `Events` table object.
  - read-model query를 만들고 default dry-run option을 생성하며
    `BigQueryContext.validateQuery`를 호출하는 helper function.
- one test source package:
  `exposed.examples.bigquery.dryrun`
  - `BigQueryDryRunWorkshopTest.kt`
  - Real Google API service chain에 대한 MockK stub:
    `Bigquery`, `Bigquery.Jobs`, `Bigquery.Jobs.Query`
  - `Jobs.query(projectId, request)`에 전달되는 실제 `QueryRequest` capture.
  - success, option mapping, generated SQL shape, BigQuery error conversion test.
- test resources:
  `junit-platform.properties` and `logback-test.xml`
- diagram assets:
  `docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.svg`
  `docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.png`

Main test는 다음과 같은 query를 생성해야 한다.

```kotlin
Events
    .select(Events.region, Events.eventId.count())
    .where { Events.revenue greaterEq BigDecimal("10.00") }
    .groupBy(Events.region)
    .orderBy(Events.region)
```

그런 다음 다음을 호출한다.

```kotlin
context.validateQuery(
    query,
    BigQueryQueryOptions(
        maximumBytesBilled = 1_000_000L,
        labels = mapOf("workshop" to "bigquery-dry-run"),
        priority = BigQueryQueryPriority.BATCH,
        location = "US",
        timeoutMs = 5_000L,
    )
)
```

Assertion은 다음을 검증해야 한다.

- `QueryRequest.dryRun == true`
- `useLegacySql == false`
- `defaultDataset`은 explicit project/dataset ID를 포함한다.
- Generated SQL이 `SELECT`, `FROM EVENTS`, `GROUP BY`, `ORDER BY`를 포함한다.
- `maximumBytesBilled`, labels, priority, location, timeout이 mapping된다.
- Successful dry run은 complete response를 반환한다.
- BigQuery error response는 clear message가 있는 `BigQueryQueryException`으로 노출된다.

## 접근 비교

### A. Mocked REST dry-run with generated SQL

권장한다. Credential이나 network access 없이 Exposed-to-BigQuery boundary를 증명한다. 또한
weekly Examples CI에 충분히 빠른 module 상태를 유지한다.

### B. BigQuery emulator

이 issue에서는 기각한다. 더 넓은 execution semantics에는 유용하지만 container lifecycle
complexity를 추가하며 이 dry-run example의 query-job option mapping 신뢰도를 높이지 않는다.

### C. Real BigQuery opt-in test

Default module에서는 기각한다. Credential, project configuration, network access, cost
guardrail이 필요하기 때문이다. 필요하면 향후 manual opt-in example을 별도로 열 수 있다.

Issue #138에서는 real BigQuery용 executable opt-in path를 추가하지 않는다. 모듈은
`GOOGLE_APPLICATION_CREDENTIALS`, Application Default Credentials, service-account file,
`GOOGLE_CLOUD_PROJECT`, endpoint override, token, API key, system property, environment
variable을 읽으면 안 되고, real Google Cloud BigQuery client를 만들면 안 된다. Mocked REST
client wiring만 허용한다.

## 위험 및 완화

- Risk: generated SQL assertion이 Exposed formatting change에 brittle해질 수 있다.
  완화: 전체 whitespace-exact SQL string 대신 durable SQL fragment를 assert한다.
- Risk: README가 real BigQuery query가 실행된다고 우발적으로 암시할 수 있다. 완화: 두
  locale 모두에서 dry-run vs execution wording을 명시적으로 사용한다.
- Risk: ADC 또는 local environment를 통해 credential이 test로 leak될 수 있다. 완화:
  test에서는 mocked BigQuery REST client만 만들고 placeholder project/dataset ID는 test constant로만
  유지하며, executable code에서 real-client 또는 environment/property access를 scan한다.
- Risk: Exposed/H2 global state가 test 사이에 leak될 수 있다. 완화: test setup에서
  deterministic H2 SQL-generation database를 만들고 각 validation을
  `BigQueryContext.validateQuery` transaction boundary 안에 유지하며, test마다 mocked client를 다시
  만들어 shared mutable mock state를 피한다.
- Risk: 새 모듈을 build하지 않아도 workflow가 green이 될 수 있다. 완화: runnable module을
  만드는 같은 PR에서 `.github/workflows/examples.yml`에 `:01-bigquery-dry-run:build`를 추가한다.
- Risk: local catalog edit로 dependency drift가 생길 수 있다. 완화: centrally governed
  `exposed-bigquery` alias만 추가하고 version은 imported `bluetape4k-dependencies` BOM에 의존한다.

## 수용 기준

- `13-ecosystem-integrations/01-bigquery-dry-run/build.gradle.kts`가 존재하고 Gradle이 project
  `:01-bigquery-dry-run`을 발견한다.
- Default test는 Google Cloud credential, ADC, network access, billable BigQuery execution
  없이 실행된다.
- Production, test, README command, example path 중 어느 것도 Google credential environment
  variable, system property, service-account file, project secret, endpoint override, token,
  API key를 읽거나 real BigQuery service/client를 만들지 않는다.
- Test는 project ID, dataset ID, location에 deterministic placeholder constant를 사용해
  `BigQueryContext`를 만들고, `Bigquery.Jobs.query`를 통해 전달된 실제 `QueryRequest`를
  MockK-capture한다.
- Test는 generated SQL fragment, dry-run flag, default dataset, billed-byte cap, label,
  priority, location, timeout, success response, failure response handling을 검증한다.
- 새 test는 JUnit 5, MockK, `bluetape4k-assertions`를 사용한다.
- 모듈에 `README.md`와 `README.ko.md`가 존재하고 language switch를 포함한다.
- README file은 dry-run validation vs query execution을 설명하고 credential-free test command를
  문서화한다.
- Diagram SVG/PNG는 `docs/images/readme-diagrams/` 아래 commit되고 두 README locale에서
  참조된다.
- Chapter README pair는 #138을 runnable로 표시하고 module README pair를 link한다.
- Root README pair는 첫 runnable chapter 13 module을 나열한다.
- `.github/workflows/examples.yml`은 `:01-bigquery-dry-run:build`를 실행한다.
- New-module registration audit는 `settings.gradle.kts`, repo-local module list, CI, Nightly,
  Examples workflow path filter/job, summary `needs`, coverage artifact, 변경이 필요 없는 곳의
  explicit N/A rationale를 기록한다.
- `actionlint .github/workflows/examples.yml` 통과.
- `./gradlew :01-bigquery-dry-run:test`와 `./gradlew :01-bigquery-dry-run:build` 통과.
- `./gradlew projects --quiet`는 `:01-bigquery-dry-run`을 포함한다.
- `git diff --check` 통과.
- PR metadata는 live issue #138 metadata에서 파생되고, #138을 close하며, parent #137을
  reference하고, #137이 계속 open임을 검증한다.
