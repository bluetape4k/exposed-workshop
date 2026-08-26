# Checkpointable Exposed JDBC Batch Workshop 설계

## 목표

이슈 [#236](https://github.com/bluetape4k/exposed-workshop/issues/236)의 범위를
JDBC 전용으로 고정하고, `bluetape4k-exposed-batch` provider가 제공하는
checkpoint·keyset restart·chunk commit·skip/retry·commit timeout·취소 상태 전이를
작은 H2 예제로 재현한다.

R2DBC 구현은 저장소의 실행 모델과 테스트 인프라가 다르므로
[exposed-r2dbc-workshop#205](https://github.com/bluetape4k/exposed-r2dbc-workshop/issues/205)에서
별도로 진행한다. 이 설계와 구현은 R2DBC 코드를 추가하거나 공통 모듈로 추상화하지 않는다.

## 현재 근거와 제약

- 저장소의 중앙 버전은 `bluetape4k-dependencies:1.4.0`이다.
- 중앙 catalog의 `bluetape4k-exposed-batch` alias는
  `io.github.bluetape4k.exposed:bluetape4k-exposed-batch:1.12.1`로 해석된다.
- provider의 `BatchStepRunner`는 `writer.write()` 성공 뒤에
  `reader.onChunkCommitted()`와 checkpoint 저장을 수행한다. 따라서 checkpoint는
  마지막으로 저장에 성공한 keyset 위치를 뜻한다.
- provider는 `ExposedJdbcBatchJobRepository`, `ExposedJdbcBatchReader`,
  `ExposedJdbcBatchWriter`와 `CheckpointJson.jackson3()`를 제공한다. batch artifact는
  Exposed JDBC와 Jackson 3를 compile-only 경계로 두므로 workshop이 필요한 runtime
  의존성을 명시적으로 선언해야 한다.
- cancellation은 `CancellationException`을 정상 실패로 변환하지 않고
  `NonCancellable` 문맥에서 `STOPPED`를 저장한 뒤 다시 던진다.
- `BatchJob`은 `RUNNING`/`FAILED`/`STOPPED` 실행을 같은 `jobName`과 parameters로
  재사용하고, 완료된 step은 다시 열지 않는다.
- 기본 검증은 외부 데이터베이스·네트워크·자격 증명 없이 H2에서 실행한다. PostgreSQL
  JDBC는 별도 opt-in 통합 경계로 남긴다.
- repository와 module 문서는 한국어 work artifact 규칙을 따르고, README와 다이어그램은
  English/Korean source-equivalent pair를 유지한다.

## 선택지와 결정

### 선택지 A — 독립 JDBC module + provider API 직접 조합 (채택)

`13-ecosystem-integrations/11-checkpointable-batch`를 새 module로 만들고,
`JdbcBatchWorkshop`이 provider의 repository·reader·writer를 직접 조합한다. 공통
`BatchJob` DSL과 타입화한 source/target table을 공개해 학습자가 checkpoint 흐름을
따라갈 수 있게 한다. 실패·timeout·cancellation 테스트에서만 작은 test writer를
주입해 provider 계약을 관찰한다.

이 선택은 provider API 자체를 숨기지 않으면서 JDBC 구현만 유지하고, 한 명의 개발자가
관리해야 하는 파일·의존성·CI 범위를 한 module로 제한한다.

### 선택지 B — 기존 `02-alternatives-to-jpa/r2dbc-example` 확장

기존 module은 Spring Data R2DBC 예제의 책임 경계를 갖고 있어 JDBC batch API를 설명하기
어렵다. Spring Data와 Exposed batch가 섞여 transaction 소유권과 재시작 계약이 흐려지므로
채택하지 않는다.

### 선택지 C — JDBC와 R2DBC sibling module을 이 저장소에 함께 추가

두 backend를 한 PR에서 비교하면 문서 parity는 쉬워 보이지만, R2DBC 테스트·pool 수명주기와
이 저장소의 JDBC 예제 규칙이 충돌한다. R2DBC는 target repository의 전용 issue로 이미
분리했으므로 중복 구현을 만들지 않는다.

## 모듈과 공개 예제 API

새 module 경로는 `13-ecosystem-integrations/11-checkpointable-batch`다.
`settings.gradle.kts`의 `includeModules("13-ecosystem-integrations", false, false)`가
자동 탐색하므로 별도 `include`는 추가하지 않는다.

예상 source 파일은
`src/main/kotlin/exposed/examples/batch/jdbc/JdbcBatchWorkshop.kt`다.

```kotlin
data class JdbcBatchOptions(
    val jobName: String = "checkpointable-jdbc-batch",
    val parameters: Map<String, Any> = mapOf("dataset" to "workshop"),
    val chunkSize: Int = 3,
    val pageSize: Int = chunkSize,
    val skipPolicy: SkipPolicy = SkipPolicy.NONE,
    val retryPolicy: RetryPolicy = RetryPolicy.NONE,
    val commitTimeout: Duration = BatchDefaults.COMMIT_TIMEOUT,
)

fun createJdbcBatchSchema(database: Database)

fun checkpointableJdbcBatchJob(
    database: Database,
    options: JdbcBatchOptions = JdbcBatchOptions(),
    processor: BatchProcessor<JdbcSourceRecord, JdbcTargetRecord> = defaultJdbcProcessor,
    writer: BatchWriter<JdbcTargetRecord> = jdbcTargetWriter(database),
): BatchJob

suspend fun runCheckpointableJdbcBatch(
    database: Database,
    options: JdbcBatchOptions = JdbcBatchOptions(),
): BatchReport
```

구현 계약은 다음과 같다.

- `JdbcBatchSourceTable`은 증가하는 `Long` key, 입력 이름, 정수 값을 가진다.
- `JdbcBatchTargetTable`은 source key를 primary key로 사용해 예제 writer가 재시작 시
  중복 행을 조기에 드러내도록 한다. 이것은 외부 side effect의 exactly-once 보장이
  아니다.
- `ExposedJdbcBatchReader`는 source table의 key column을 오름차순 keyset으로 읽고,
  `JdbcSourceRecord.id`를 checkpoint key로 반환한다.
- `ExposedJdbcBatchWriter`는 target table에 chunk를 `batchInsert`한다. 기본 writer는
  idempotency와 at-least-once 경계를 README에서 명시한다.
- `CheckpointJson.jackson3()`를 `ExposedJdbcBatchJobRepository`에 주입해 Long
  checkpoint의 타입 round-trip을 보장한다. `toString()` fallback은 사용하지 않는다.
- `processor`와 `writer` 주입 지점은 테스트가 provider의 skip/retry/timeout/cancel
  계약을 검증하기 위한 최소 확장점이다. 새로운 adapter 계층이나 Spring Batch DSL은
  추가하지 않는다.
- `JdbcBatchOptions`는 immutable 설정으로 유지하고 양의 `chunkSize`/`pageSize`, blank가
  아닌 `jobName`을 즉시 거부한다.

## 의존성과 Gradle 구성

`gradle/libs.versions.toml`에 BOM이 버전을 관리하는 alias를 추가한다.

```toml
exposed-batch = { module = "io.github.bluetape4k.exposed:bluetape4k-exposed-batch" }
```

module `build.gradle.kts`는 다음 경계를 사용한다.

- `implementation(libs.exposed.batch)` — provider batch API
- `implementation(libs.jetbrains.exposed.core)`와 `implementation(libs.jetbrains.exposed.jdbc)` —
  provider의 compile-only Exposed 계약을 보충
- `implementation(libs.bluetape4k.jackson3)` — `CheckpointJson.jackson3()` runtime
- `implementation(libs.bluetape4k.virtualthread.jdk25)` — 현재 repository의 JDK 25 runtime
  선택을 명시
- `runtimeOnly(libs.h2.v2)` — 기본 결정적 테스트/예제 database
- `testImplementation(libs.bluetape4k.junit5)`와 `testImplementation(libs.kotlinx.coroutines.test)` —
  기존 repository test convention 재사용

새 외부 dependency나 별도 version pin은 추가하지 않는다. provider가 끌어오는 legacy
`bluetape4k-virtualthread-jdk21`가 test runtime에 섞이지 않도록 module configuration에서
제외하고 JDK 25 variant를 고정한다.

## 테스트 설계

테스트 파일은
`src/test/kotlin/exposed/examples/batch/jdbc/JdbcBatchWorkshopTest.kt`다.
각 테스트는 고유 H2 in-memory URL을 사용하고 `SchemaUtils.create`로 batch metadata와
source/target table을 함께 만든다. 테스트 간 상태를 공유하지 않으며 외부 container는
기본 경로에 포함하지 않는다.

검증 시나리오는 다음과 같다.

1. 정상 실행: source 8건을 chunk 3으로 읽고 target 8건을 저장하며 `BatchReport.Success`,
   `COMPLETED`, read/write count와 마지막 checkpoint를 검증한다.
2. 실패 상태 경계: 첫 chunk 뒤 writer가 한 번 실패해 `FAILED`를 남긴다. provider의
   현재 FAILED report가 checkpoint를 비워 저장하는 동작은 [bluetape4k-exposed#745](https://github.com/bluetape4k/bluetape4k-exposed/issues/745)의
   후속 결함으로 연결하고 workshop에 workaround를 추가하지 않는다. 실제 keyset
   restart는 STOPPED 재실행 시나리오에서 검증한다.
3. processor skip: 짝수 입력에서 예외를 던지는 processor와 `SkipPolicy.ALL`을 사용해
   `COMPLETED_WITH_SKIPS`, skip count, 저장된 홀수 결과를 확인한다.
4. writer retry/backoff: 첫 write만 실패하는 writer와 짧은 `RetryPolicy`를 사용해
   재시도 횟수와 최종 성공을 검증한다. 실제 대기 시간은 millisecond 이하로 제한한다.
5. commit timeout: 지연 writer와 짧은 `commitTimeout`, `SkipPolicy.maxSkips`를 조합해
   timeout이 chunk-level skip으로 기록되고 target에 부분 write가 남지 않는지 확인한다.
6. cancellation/STOPPED: 첫 chunk checkpoint가 저장된 뒤 다음 write에서 대기하는
   writer를 취소한다. 호출자에게 `CancellationException`이 전파되고 metadata가
   `STOPPED`가 되며, 같은 parameters로 재실행할 때 이미 커밋된 첫 chunk를 중복 처리하지
   않고 나머지를 완료하는지 확인한다.
7. schema contract: `batch_job_execution`, `batch_step_execution`의 상태·checkpoint
   column과 source/target primary key가 생성되는지 확인한다.

실패·취소 테스트의 test writer는 데이터베이스를 직접 조작하지 않고, 정상 writer를
위임하거나 지연·예외만 주입한다. 이렇게 해야 checkpoint 순서와 provider 상태 전이를
workshop API에서 그대로 관찰할 수 있다.

## 문서와 다이어그램

module에는 다음 source-equivalent 문서를 추가한다.

- `13-ecosystem-integrations/11-checkpointable-batch/README.md`
- `13-ecosystem-integrations/11-checkpointable-batch/README.ko.md`

두 README는 같은 순서로 목표, dependency, source/target schema, job 구성, checkpoint
복원, failure/restart, skip/retry/timeout, `STOPPED` cancellation, 테스트 명령과
exactly-once 제외 범위를 설명한다. README에는 raw Mermaid를 넣지 않고 PNG만 embed한다.

다이어그램은 “한 chunk의 write 성공과 checkpoint 저장 순서가 재시작 위치와 어떻게
연결되는가?”를 답해야 한다. source-equivalent English/Korean asset pair를 만든다.

- `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-architecture-01.svg`
- `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-architecture-01.png`
- `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-architecture-01.ko.svg`
- `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-architecture-01.ko.png`
- `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-lifecycle-01.svg`
- `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-lifecycle-01.png`
- `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-lifecycle-01.ko.svg`
- `docs/images/readme-diagrams/13-checkpointable-jdbc-batch-lifecycle-01.ko.png`

architecture diagram은 workshop API, `BatchJob`/`BatchStepRunner`, JDBC reader/writer,
repository metadata table, H2와 재시작 경계를 표시한다. lifecycle diagram은
`RUNNING → FAILED/STOPPED → RUNNING → COMPLETED(_WITH_SKIPS)`와
`write → onChunkCommitted → saveCheckpoint` 순서를 표시한다. SVG source를 먼저 만들고
PNG를 렌더링한 뒤 diagram asset audit와 시각 검사를 수행한다.

Chapter 13 README와 root `README.md`/`README.ko.md`에는 module 경로, `:11-checkpointable-batch:build`,
issue #236, 양쪽 locale 링크를 module 파일이 존재한 뒤 추가한다.

## Workflow와 CI 경계

- `.github/scripts/select-changed-examples.sh`의 고정 `ALL_TASKS`에
  `:11-checkpointable-batch:build`를 추가한다. chapter 13 path mapping은 기존 동적
  규칙을 재사용한다.
- `.github/workflows/examples.yml`의 기존 `13-ecosystem-integrations/**` path filter를
  유지하고, module 변경 시 선택 task가 실제로 `:11-checkpointable-batch:build`를
  반환하는지 검사한다.
- workflow lane은 Weekly Examples다. H2 기본 테스트만 CI에서 실행하고 PostgreSQL
  JDBC는 명시적 opt-in으로 남긴다.
- worktree branch는 `feat/issue-236-checkpointable-batch`를 사용한다. 한 명의 개발자가
  설계 → 구현 → 검증을 순차 수행하며, 독립 병렬 lane이나 별도 human review gate를
  추가하지 않는다.

## 수용 기준

- `exposed-batch` alias와 module이 `bluetape4k-dependencies:1.4.0`에서 해석된다.
- H2 JDBC 정상 실행, FAILED 상태 경계, processor skip, writer retry/backoff,
  commit timeout, cancellation `STOPPED`와 keyset 재실행이 테스트로 고정된다.
- FAILED checkpoint 보존 결함은 [bluetape4k-exposed#745](https://github.com/bluetape4k/bluetape4k-exposed/issues/745)에 연결하고
  이 workshop에서 우회 adapter를 만들지 않는다.
- batch metadata table과 source/target schema가 README에서 실제 이름과 상태 전이로
  설명된다.
- R2DBC 코드는 이 저장소에 없고 target issue #205로 연결된다.
- English/Korean README가 source-equivalent이고 architecture/lifecycle SVG/PNG pair가
  존재한다.
- module test, detekt/static 검사, changed-examples task selection과 `git diff --check`가
  통과한다.

## 리뷰 렌즈 기록

| 렌즈 | 확인 내용 |
|---|---|
| API/아키텍처 | provider API를 숨기지 않는 JDBC job builder와 저장소별 책임 경계 |
| Kotlin 패턴 | immutable options, null-safety, `suspend` API, Korean KDoc, 불변 checkpoint key |
| 테스트 | H2 deterministic RED→GREEN, 실패/취소 주입, 재시작 count와 상태 read-back |
| 보안/운영 | credentials/network 없음, at-least-once와 idempotency 경계, timeout/lease caveat |
| 문서/다이어그램 | README EN/KO parity, PNG embed, SVG source, lifecycle/connector 감사 |
| 범위/워크플로 | JDBC 단독, target R2DBC issue link, chapter 13 등록, Examples task, single-owner 순차 실행 |

## SPW writer gate

- [x] SPW-01 — artifact audience는 workshop learner/contributor, 목적은 JDBC-only 설계 고정, 근거는 issue #236, provider `1.12.1`, central BOM `1.4.0`, local module/CI 구조이며 R2DBC는 unresolved implementation으로 명시했다.
- [x] SPW-02 — 선택지, 경계, API, 의존성, 실패 모드, 호환성, acceptance와 DoD를 포함했다.
- [x] SPW-03 — Korean technical register를 적용하고 API/identifier/command/URL/version을 보존했다.
- [x] SPW-04 — provider source/README, local Gradle catalog, chapter 13 workflow와 대조해 claim을 고정했다.
- [x] SPW-05 — 파일을 저장한 뒤 Markdown headings, code fence, 목록, 링크 parity와 terminology audit를 다시 읽고 workflow receipt에 기록한다.
