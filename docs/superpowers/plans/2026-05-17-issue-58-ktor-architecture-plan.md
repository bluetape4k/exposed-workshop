# Issue 58 - Ktor Application Architecture Plan

## Scope

Implement the first chapter 12 Ktor module in `exposed-workshop`.

## Tasks

1. Add design and advisor artifacts.
   - Complexity: S
   - Apply `bluetape4k-design` review gates and Claude Code CLI advisor review.

2. Add Gradle/catalog wiring.
   - Complexity: S
   - Add `ktor = "3.4.3"` and aliases:
     `ktor-server-core`, `ktor-server-cio`,
     `ktor-server-content-negotiation`, `ktor-server-status-pages`,
     `ktor-server-call-logging`, `ktor-server-call-id`,
     `ktor-server-test-host`, and `ktor-serialization-kotlinx-json`.
   - Register `12-production-integration`.
   - Add module `build.gradle.kts`.

3. Implement Ktor + Exposed baseline.
   - Complexity: M
   - Add table, repository, service, DTOs, routes, application module, and main.
   - Repository API is `suspend`; repository owns the
     `withContext(Dispatchers.IO) { transaction {} }` boundary.
   - Use deterministic Hikari/H2 settings for tests, including
     `maximumPoolSize = 4`.
   - Add explicit `StatusPages` mappings for validation, not-found, malformed
     request, and sanitized fallback errors. Do not echo exception messages or
     stack traces from unexpected exceptions.
   - Configure request body limit at `64 KiB`.
   - Configure CallId logging with MDC-friendly logback test output.
   - Apply `bluetape4k-patterns`: no `!!`, validate caller input with
     `require*`/explicit exceptions, keep state immutable.
   - Do not use `runCatching` around suspend calls.

4. Add focused tests.
   - Complexity: M
   - Use Ktor `testApplication`.
   - Use bluetape4k assertions, not JUnit/kotlin.test assertions.
   - Do not use `assertThrows`, `invoking { } shouldThrow`, or
     `kotlin.test.assertFailsWith`.
   - Use a unique H2 JDBC URL per test and assert empty table state at test
     start.
   - Add a parallel insert smoke test with 16 concurrent requests and verify
     distinct primary keys plus total row count.

5. Add documentation.
   - Complexity: S
   - Add `README.md` and `README.ko.md`.
   - Include a small Mermaid architecture diagram.
   - Document run/test commands, the suspend repository boundary, and why the
     R2DBC workshop is the non-blocking persistence counterpart.

6. Verify.
   - Complexity: S
   - Run `./gradlew projects`.
   - Run `./gradlew :01-ktor-application-architecture:compileKotlin`.
   - Run `./gradlew :01-ktor-application-architecture:test`.
   - Run `./gradlew :01-ktor-application-architecture:detekt` if the task is
     available for the module.
   - Run `git diff --check`.

7. Capture lesson and commit.
   - Complexity: S
   - Add `docs/lessons/2026-05-17-issue-58-ktor-architecture.md`.
   - Commit with Lore trailers after verification.

## Advisor Review Requirement

Claude Code CLI review artifacts:

- `.omx/artifacts/claude-issue-58-spec-plan-2026-05-17.md`
- `.omx/artifacts/claude-issue-58-spec-plan-rereview-2026-05-17.md`

Summarize accepted/rejected findings in the research note.
