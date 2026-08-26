# Issue #236 checkpointable JDBC batch 예제 리뷰

## 리뷰 범위

- 기준 이슈: [#236](https://github.com/bluetape4k/exposed-workshop/issues/236)
- 대상: `13-ecosystem-integrations/11-checkpointable-batch`
- 경계: 이 저장소에는 JDBC만 구현하고 R2DBC는
  [exposed-r2dbc-workshop#205](https://github.com/bluetape4k/exposed-r2dbc-workshop/issues/205)에서 구현한다.
- 운영 제약: 단일 개발자, 순차 실행, H2 기본 경로, 원격 DB smoke 비활성

## 결과

| 등급 | 개수 | 상태 |
|---|---:|---|
| P0 | 0 | 차단 사항 없음 |
| P1 | 0 | 차단 사항 없음 |
| P2 | 0 | 범위 밖 항목을 문서화하고 후속 이슈로 연결 |

구현·테스트·문서·등록 사슬은 `CLEAR`다. provider의 FAILED checkpoint 보존
결함은 workshop workaround로 숨기지 않고
[bluetape4k-exposed#745](https://github.com/bluetape4k/bluetape4k-exposed/issues/745)로
분리했다.

## 여섯 렌즈 점검

1. **API/아키텍처** — `JdbcBatchSourceTable`과 `JdbcBatchTargetTable`,
   keyset `ExposedJdbcBatchReader`, `ExposedJdbcBatchWriter`,
   `ExposedJdbcBatchJobRepository`를 provider DSL에서 직접 조합한다. 공개
   기본 processor/writer와 options는 테스트에서 주입할 수 있다.
2. **Kotlin/Exposed 패턴** — 불변 data class와 명시적 `org.jetbrains.exposed.v1.*`
   import를 사용한다. writer의 `BatchInsertStatement` receiver는 테이블
   컬럼을 명시적으로 한정하며 deprecated Exposed import는 없다.
3. **테스트** — 정상 완료, FAILED 경계, processor skip, bounded retry,
   commit timeout, cancellation/STOPPED restart, schema/options를 H2에서
   검증한다. `runSuspendIO`를 사용해 provider virtual-thread dispatcher와
   테스트 dispatcher의 timeout 충돌을 피했다.
4. **안정성/운영** — chunk commit 뒤 checkpoint를 저장하고, cancellation은
   `STOPPED`와 `CancellationException`을 함께 확인한다. target primary key가
   재시작 중 중복 write를 드러내며 exactly-once를 가장하지 않는다.
5. **문서/시각 자산** — English/Korean module README가 같은 코드·링크·상태
   토큰을 설명하고, architecture/lifecycle SVG·PNG가 같은 semantic ledger를
   공유한다. README는 PNG를 embed하고 SVG source 링크를 제공한다.
6. **등록/사용자 경계** — central catalog, 자동 module discovery, chapter/root
   README, Examples selector, test resources가 함께 등록됐다. 모듈 단독 변경은
   `:11-checkpointable-batch:build`로 선택된다.

## 검증 근거

- `USE_FAST_DB=true ./gradlew :11-checkpointable-batch:test --no-daemon --rerun-tasks` — `8 passing`.
- `./gradlew :11-checkpointable-batch:build --no-daemon --rerun-tasks` — `BUILD SUCCESSFUL`.
- `./gradlew :11-checkpointable-batch:koverXmlReport --no-daemon --rerun-tasks` — `BUILD SUCCESSFUL`.
- `./gradlew detekt --no-daemon` — `exit_code=0`, module detekt 포함.
- `./gradlew projects --no-daemon` — `:11-checkpointable-batch` 발견.
- module-only synthetic diff selector — `all=false`, `module_tasks=:11-checkpointable-batch:build`.
- Exposed import/receiver 검사, Korean terminology audit, `git diff --check` — 통과.
- 양 언어 diagram XML/text/connector/arrowhead/geometry/endpoint/mixed-corner/
  visual/semantic audit 및 scoped asset-pair audit — 통과.

## 잔여 위험

- PostgreSQL JDBC와 원격 provider smoke는 자격 증명·endpoint가 필요한 opt-in
  경계이므로 실행하지 않았다.
- provider FAILED checkpoint 보존은 #745가 해결되기 전까지 이 예제에서
  보장하지 않는다.

## 판정

`P0=0, P1=0, P2=0` — 구현은 merge-ready 후보이다. 최종 커밋·push 후 exact
head의 PR body, CI, review thread, mergeability를 다시 읽고 fresh merge
approval 전까지 delivery 상태는 `PENDING`으로 유지한다.
