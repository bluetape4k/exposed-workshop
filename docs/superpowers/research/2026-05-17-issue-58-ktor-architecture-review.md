# Issue 58 - Spec/Plan 검토

## Codex 6-Tier 검토

| Tier | 발견 사항 | 우선순위 | 결정 |
|---|---|---:|---|
| Security | Stack trace 또는 구현 세부가 노출되지 않도록 error response와 malformed JSON 처리를 명시해야 한다. | P1 | 수용. StatusPages mapping과 JSON/body 제약을 추가한다. |
| Ops/SRE | Blocking Exposed JDBC는 Ktor call/event-loop thread에서 실행하면 안 된다. | P0 | 수용. Repository가 suspend API와 `Dispatchers.IO` transaction boundary를 소유한다. |
| Structural | Version catalog alias는 repo-wide dependency 이름에 영향을 준다. | P2 | 수용. 기존 bluetape4k Ktor 예제에서 복사한 namespaced `ktor-*` alias를 사용한다. |
| Kotlin Quality | Route layer는 얇게 유지하고, validation은 service에, persistence는 repository에 둔다. | P1 | 수용. Spec/plan은 route-service-repository boundary를 요구한다. |
| Tests/Types | H2 database isolation과 concurrency smoke coverage가 충분히 구체화되지 않았다. | P0 | 수용. Test별 고유 H2 URL과 parallel insert smoke test를 사용한다. |
| Performance/Stability | Hikari pool과 blocking dispatcher 동작에는 결정적인 test sizing이 필요하다. | P2 | 수용. Test pool size를 고정하고 JDBC boundary를 문서화한다. |

계획 수정 후 Codex gate 결과: P0 = 0, P1 = 0.

## Claude Code Opus 자문

Artifact: `.omx/artifacts/claude-issue-58-spec-plan-2026-05-17.md`

| 우선순위 | 발견 사항 | 결정 | 후속 조치 |
|---|---|---|---|
| P0 | Ktor event loop에서 blocking JDBC가 실행된다. | 수용 | `Dispatchers.IO` transaction boundary를 가진 suspend repository API를 추가한다. |
| P0 | H2 test isolation이 충분히 구체화되지 않았다. | 수용 | Test별 고유 H2 URL과 empty-state assertion을 요구한다. |
| P0 | Concurrency smoke test가 빠져 있다. | 수용 | Parallel insert route test를 추가한다. |
| P1 | StatusPages mapping이 명시되지 않았다. | 수용 | Exception-to-status mapping을 명시한다. |
| P1 | JSON/body limit이 빠져 있다. | 수용 | JSON config와 request size limit 요구사항을 추가한다. |
| P1 | CallLogging/CallId baseline이 빠져 있다. | 수용 | Design에 plugin을 추가한다. |
| P1 | Repository는 suspend API를 노출해야 한다. | 수용 | Blocking boundary를 repository 안으로 옮긴다. |
| P2/P3 | Alias naming, negative path, pool sizing, KDoc, assertion rule. | 수용 | Plan과 verification criteria에 추가한다. |

계획 수정 후 review gate 결과: P0 = 0, P1 = 0.

### Claude Code Opus 재검토

Artifact: `.omx/artifacts/claude-issue-58-spec-plan-rereview-2026-05-17.md`

판정: PASS. P0 = 0, P1 = 0.

| 우선순위 | 발견 사항 | 결정 | 후속 조치 |
|---|---|---|---|
| P2 | Body limit, pool size, parallel smoke count를 고정해야 한다. | 수용 | Plan은 이제 64 KiB, pool size 4, 16 concurrent request를 사용한다. |
| P2 | Sanitized 500은 exception detail을 echo하면 안 된다. | 수용 | Plan에 이를 명시했다. |
| P3 | CallId/logback과 README diagram 세부를 포함한다. | 수용 | Plan을 갱신했다. |
| P3 | 구현 시 suspend call 주변에 `runCatching`을 쓰지 말라는 reminder가 필요하다. | 수용 | Plan을 갱신했다. |

## Claude Code 구현 검토

Artifact: `.omx/artifacts/claude-code-review-issue-58-2026-05-17.md`

판정: FAIL. P0 = 0, P1 = 2.

| 우선순위 | 발견 사항 | 결정 | 후속 조치 |
|---|---|---|---|
| P1 | Catch-all 500 handler가 response는 sanitize했지만 root cause를 log하지 않았다. | 수용 | `INTERNAL_ERROR`를 반환하기 전에 unexpected exception을 log하고 cancellation은 다시 던진다. |
| P1 | Request body limit이 `Content-Length`만 검사해서 length가 없거나 chunked인 요청이 guard를 우회할 수 있었다. | 수용 | JSON parsing 전에 declared/actual UTF-8 body size를 모두 검사하는 helper로 decode한다. |
| P2 | JetBrains Exposed와 bluetape4k Exposed dependency가 중복된다. | 수용 | 이 작은 workshop module에서는 direct JetBrains Exposed dependency를 유지한다. |
| P2 | `prettyPrint = true`는 production-shaped JSON default가 아니다. | 수용 | `ignoreUnknownKeys`와 `encodeDefaults`를 둔 compact JSON을 사용한다. |
| P2 | Generic `IllegalArgumentException` message는 echo하면 안 된다. | 수용 | 작성자가 제어한 validation message용 local validation exception을 추가하고 generic `IllegalArgumentException`은 sanitize한다. |
| P3 | Parallel insert test에 중복된 약한 assertion이 있었다. | 수용 | Distinct-ID set assertion과 row-count assertion을 유지한다. |

### Claude Code 구현 재검토

Artifact: `.omx/artifacts/claude-code-review-rereview-issue-58-2026-05-17.md`

판정: FAIL. P0 = 0, P1 = 1.

| 우선순위 | 발견 사항 | 결정 | 후속 조치 |
|---|---|---|---|
| P1 | `receiveText()` 뒤에 actual size를 확인하면 chunked 또는 length가 없는 request가 먼저 unbounded buffering될 수 있다. | 수용 | `receiveText()`를 `receiveChannel()`로 바꾸고, chunk를 bounded buffer로 읽는 동안 64 KiB limit을 강제한다. |

### Claude Code 최종 재검토

Artifact: `.omx/artifacts/claude-code-review-final-issue-58-2026-05-17.md`

판정: PASS. P0 = 0, P1 = 0.

| 점검 | 결과 |
|---|---|
| Streaming body limit | 초과분을 buffering하기 전에 chunk read 중 64 KiB가 강제된다. |
| Fallback 500 | Unexpected exception은 log되고 response는 sanitize 상태를 유지한다. |
| Cancellation | `CancellationException`은 logging/responding 전에 다시 던져진다. |
| JDBC boundary | Exposed JDBC는 repository-owned `Dispatchers.IO` transaction boundary 뒤에 유지된다. |
