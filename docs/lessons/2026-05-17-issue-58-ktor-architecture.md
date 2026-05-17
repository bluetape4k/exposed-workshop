# Issue 58 - Ktor Architecture Baseline

## Context

Chapter 12 starts production integration examples. The first module needed a
small Ktor baseline before adding auth, outbox, client, or observability
examples.

## Decision

Use a Ktor + Exposed JDBC module with explicit route/service/repository
boundaries. Keep blocking Exposed transactions inside a suspend repository API
that wraps `transaction {}` with `Dispatchers.IO`.

## Outcome

Added `12-production-integration/01-ktor-application-architecture` with Ktor
JSON, StatusPages, CallId/CallLogging, H2 persistence, route tests, and
English/Korean README files.

Claude implementation review caught two issues after the first commit: the
fallback 500 handler did not log root causes, and the body limit only trusted
`Content-Length`. A first fix checked size after `receiveText()`, but Claude
correctly rejected it because the body was already buffered. The final version
logs unexpected failures and enforces the size limit while streaming request
body chunks before JSON decoding.

## Verification

- `./gradlew -q projects`
- `./gradlew :01-ktor-application-architecture:compileKotlin`
- `./gradlew :01-ktor-application-architecture:compileTestKotlin`
- `./gradlew :01-ktor-application-architecture:test` - 8 passing
- `./gradlew --offline :01-ktor-application-architecture:compileKotlin :01-ktor-application-architecture:compileTestKotlin :01-ktor-application-architecture:test` - 8 passing after review fixes
- `./gradlew detekt` - `NO-SOURCE`
- `git diff --check`
- Claude Code implementation final re-review - PASS, P0 = 0, P1 = 0

IDE batch diagnostics reported zero problems after opening the project through
IntelliJ, but the CLI diagnostics were not editor-fresh. Online Gradle
resolution was blocked by an external snapshot POM for
`io.github.bluetape4k.aws:bluetape4k-aws-bom:0.1.0`; the same targeted compile
and test verification passed with `--offline`. CI workflow changes were not
required: daily CI runs repository-wide Gradle tests, and the new H2-only module
is included through `settings.gradle.kts`.

## Future Guidance

For future Ktor + JDBC examples, do not call Exposed `transaction {}` directly
from routes. Put the blocking boundary in the repository and test at least one
parallel write path when the example demonstrates service architecture.
Do not rely on `Content-Length` alone for request limits; also enforce the limit
against the bytes actually read.
