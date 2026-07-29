# Issue 58 - Ktor application architecture 계획

## 범위

`exposed-workshop`의 첫 12장 Ktor module을 구현한다.

## 작업

1. 설계와 advisor artifact를 추가한다.
   - Complexity: S
   - `bluetape4k-design` review gate와 Claude Code CLI advisor review를 적용한다.

2. Gradle/catalog wiring을 추가한다.
   - Complexity: S
   - `ktor = "3.4.3"` 및 다음 alias를 추가한다:
     `ktor-server-core`, `ktor-server-cio`,
     `ktor-server-content-negotiation`, `ktor-server-status-pages`,
     `ktor-server-call-logging`, `ktor-server-call-id`,
     `ktor-server-test-host`, and `ktor-serialization-kotlinx-json`.
   - `12-production-integration`을 등록한다.
   - Module `build.gradle.kts`를 추가한다.

3. Ktor + Exposed baseline을 구현한다.
   - Complexity: M
   - Table, repository, service, DTO, route, application module, main을 추가한다.
   - Repository API는 `suspend`다. Repository가 `withContext(Dispatchers.IO) { transaction {} }`
     boundary를 소유한다.
   - Test에는 `maximumPoolSize = 4`를 포함한 deterministic Hikari/H2 setting을 사용한다.
   - Validation, not-found, malformed request, sanitized fallback error를 위한 명시적
     `StatusPages` mapping을 추가한다. Unexpected exception의 message나 stack trace를 echo하지
     않는다.
   - Request body limit을 `64 KiB`로 설정한다.
   - MDC-friendly logback test output과 함께 CallId logging을 설정한다.
   - `bluetape4k-patterns`를 적용한다: `!!` 없음, caller input은 `require*`/explicit exception으로
     검증, state는 immutable하게 유지한다.
   - Suspend call 주변에는 `runCatching`을 사용하지 않는다.

4. 집중 test를 추가한다.
   - Complexity: M
   - Ktor `testApplication`을 사용한다.
   - JUnit/kotlin.test assertion이 아니라 bluetape4k assertion을 사용한다.
   - `assertThrows`, `invoking { } shouldThrow`, `kotlin.test.assertFailsWith`를 사용하지 않는다.
   - Test마다 unique H2 JDBC URL을 사용하고 test 시작 시 table이 비어 있음을 assert한다.
   - 16개 concurrent request를 사용하는 parallel insert smoke test를 추가하고 distinct primary key와
     total row count를 검증한다.

5. 문서를 추가한다.
   - Complexity: S
   - `README.md`와 `README.ko.md`를 추가한다.
   - 작은 Mermaid architecture diagram을 포함한다.
   - 실행/test command, suspend repository boundary, R2DBC workshop이 non-blocking persistence
     counterpart인 이유를 문서화한다.

6. 검증한다.
   - Complexity: S
   - `./gradlew projects`를 실행한다.
   - `./gradlew :01-ktor-application-architecture:compileKotlin`을 실행한다.
   - `./gradlew :01-ktor-application-architecture:test`를 실행한다.
   - Module에서 task를 사용할 수 있으면 `./gradlew :01-ktor-application-architecture:detekt`를
     실행한다.
   - `git diff --check`를 실행한다.

7. Lesson을 기록하고 commit한다.
   - Complexity: S
   - `docs/lessons/2026-05-17-issue-58-ktor-architecture.md`를 추가한다.
   - 검증 후 Lore trailer를 포함해 commit한다.

## Advisor review 요구사항

Claude Code CLI review artifacts:

- `.omx/artifacts/claude-issue-58-spec-plan-2026-05-17.md`
- `.omx/artifacts/claude-issue-58-spec-plan-rereview-2026-05-17.md`

Research note에 accepted/rejected finding을 요약한다.
