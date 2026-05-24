# Issue 49 Ktor Routing DataSource

## Context

Issue #49 asked for a Ktor routing datasource example near the existing Spring routing datasource module.

## Decision

Use a Ktor plugin to select `READ` or `WRITE` from request method or `X-Data-Source`, then bind that role through a coroutine context element before repository access.

## Outcome

Added `11-high-performance/07-routing-datasource-ktor` with two H2-backed datasource roles, observable route responses, selection counters, English/Korean README files, and a rendered architecture diagram.

## Verification

Passed: `repo-test-summary -- ./gradlew :07-routing-datasource-ktor:test` with five passing routing selection tests.

## Future Guidance

Routing datasource examples should expose the selected role in tests, because it is easier to verify than inferring routing from side effects.
