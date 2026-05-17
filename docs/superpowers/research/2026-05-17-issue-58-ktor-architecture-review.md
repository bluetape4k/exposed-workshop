# Issue 58 - Spec/Plan Review

## Codex 6-Tier Review

| Tier | Finding | Priority | Decision |
|---|---|---:|---|
| Security | Error responses and malformed JSON handling must be explicit to avoid stack trace or implementation-detail leaks. | P1 | Accept; add StatusPages mapping and JSON/body constraints. |
| Ops/SRE | Blocking Exposed JDBC must not run on Ktor call/event-loop threads. | P0 | Accept; repository owns suspend API and `Dispatchers.IO` transaction boundary. |
| Structural | Version catalog aliases affect repo-wide dependency names. | P2 | Accept; use namespaced `ktor-*` aliases copied from existing bluetape4k Ktor examples. |
| Kotlin Quality | Route layer should stay thin; validation belongs in service; persistence belongs in repository. | P1 | Accept; spec/plan now require route-service-repository boundary. |
| Tests/Types | H2 database isolation and concurrency smoke coverage were under-specified. | P0 | Accept; use unique H2 URL per test and parallel insert smoke test. |
| Performance/Stability | Hikari pool and blocking dispatcher behavior need deterministic test sizing. | P2 | Accept; pin test pool size and document JDBC boundary. |

Codex gate result after planned edits: P0 = 0, P1 = 0.

## Claude Code Opus Advisor

Artifact: `.omx/artifacts/claude-issue-58-spec-plan-2026-05-17.md`

| Priority | Finding | Decision | Follow-up |
|---|---|---|---|
| P0 | Blocking JDBC on Ktor event loop. | Accepted | Add suspend repository API with `Dispatchers.IO` transaction boundary. |
| P0 | H2 test isolation under-specified. | Accepted | Require unique H2 URL per test and empty-state assertion. |
| P0 | Missing concurrency smoke test. | Accepted | Add parallel insert route test. |
| P1 | StatusPages mapping unspecified. | Accepted | Add explicit exception-to-status mapping. |
| P1 | Missing JSON/body limits. | Accepted | Add JSON config and request size limit requirement. |
| P1 | Missing CallLogging/CallId baseline. | Accepted | Add plugins to design. |
| P1 | Repository should expose suspend API. | Accepted | Move blocking boundary into repository. |
| P2/P3 | Alias naming, negative paths, pool sizing, KDoc, assertion rules. | Accepted | Add to plan and verification criteria. |

Review gate result after planned edits: P0 = 0, P1 = 0.

### Claude Code Opus Re-Review

Artifact: `.omx/artifacts/claude-issue-58-spec-plan-rereview-2026-05-17.md`

Verdict: PASS. P0 = 0, P1 = 0.

| Priority | Finding | Decision | Follow-up |
|---|---|---|---|
| P2 | Pin body limit, pool size, and parallel smoke count. | Accepted | Plan now uses 64 KiB, pool size 4, and 16 concurrent requests. |
| P2 | Sanitized 500 must not echo exception details. | Accepted | Plan now states this explicitly. |
| P3 | Include CallId/logback and README diagram details. | Accepted | Plan updated. |
| P3 | Remind implementation not to use `runCatching` around suspend calls. | Accepted | Plan updated. |

## Claude Code Implementation Review

Artifact: `.omx/artifacts/claude-code-review-issue-58-2026-05-17.md`

Verdict: FAIL. P0 = 0, P1 = 2.

| Priority | Finding | Decision | Follow-up |
|---|---|---|---|
| P1 | Catch-all 500 handler sanitized the response but did not log the root cause. | Accepted | Log unexpected exceptions before returning `INTERNAL_ERROR`; rethrow cancellation. |
| P1 | Request body limit only checked `Content-Length`, so missing/chunked length could bypass the guard. | Accepted | Decode through a helper that checks both declared and actual UTF-8 body size before JSON parsing. |
| P2 | Duplicate JetBrains Exposed and bluetape4k Exposed dependencies. | Accepted | Keep the direct JetBrains Exposed dependencies for this small workshop module. |
| P2 | `prettyPrint = true` is not a production-shaped JSON default. | Accepted | Use compact JSON with `ignoreUnknownKeys` and `encodeDefaults`. |
| P2 | Generic `IllegalArgumentException` messages should not be echoed. | Accepted | Add a local validation exception for authored validation messages and sanitize generic `IllegalArgumentException`. |
| P3 | Parallel insert test had a redundant weak assertion. | Accepted | Keep the distinct-ID set assertion and row-count assertion. |

### Claude Code Implementation Re-Review

Artifact: `.omx/artifacts/claude-code-review-rereview-issue-58-2026-05-17.md`

Verdict: FAIL. P0 = 0, P1 = 1.

| Priority | Finding | Decision | Follow-up |
|---|---|---|---|
| P1 | Checking the actual size after `receiveText()` still lets a chunked or missing-length request buffer unbounded data first. | Accepted | Replace `receiveText()` with `receiveChannel()` and enforce the 64 KiB limit while streaming chunks into a bounded buffer. |

### Claude Code Final Re-Review

Artifact: `.omx/artifacts/claude-code-review-final-issue-58-2026-05-17.md`

Verdict: PASS. P0 = 0, P1 = 0.

| Check | Result |
|---|---|
| Streaming body limit | 64 KiB is enforced during chunk reads before buffering excess data. |
| Fallback 500 | Unexpected exceptions are logged and the response remains sanitized. |
| Cancellation | `CancellationException` is rethrown before logging/responding. |
| JDBC boundary | Exposed JDBC remains behind the repository-owned `Dispatchers.IO` transaction boundary. |
