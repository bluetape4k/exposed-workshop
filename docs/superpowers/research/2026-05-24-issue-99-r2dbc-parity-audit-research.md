# Issue 99 R2DBC Parity Audit

## Context

Issue [#99](https://github.com/bluetape4k/exposed-workshop/issues/99) asks for a concept-level parity audit between
`exposed-workshop` and `exposed-r2dbc-workshop`. The target is not exact module-name parity. It is to decide whether
each roadmap topic is covered by the counterpart repo, intentionally platform-specific, or a real gap that needs a
follow-up issue.

The R2DBC-side counterpart is [exposed-r2dbc-workshop#89](https://github.com/bluetape4k/exposed-r2dbc-workshop/issues/89)
(closed; mirrors this decision).

## Current issue state on 2026-05-24

| Repository | Open example roadmap issues | Relevant closed roadmap issues |
|------------|-----------------------------|--------------------------------|
| `exposed-workshop` | `#99` only | `#45`-`#63` |
| `exposed-r2dbc-workshop` | None | `#32`-`#49`, `#69`, `#89` |

## Parity decisions

| `exposed-workshop` topic | R2DBC counterpart | Decision |
|--------------------------|-------------------|----------|
| Ktor examples epic `#45` and multi-tenant `#46` | R2DBC `#32`, `#33`; `10-multi-tenant/07-multitenant-ktor` | Covered by counterpart |
| Ktor cache/routing implementation issues `#47`, `#48`, `#49` plus wiring `#50`; JDBC modules `11-high-performance/05-07-*` | R2DBC `#34`, `#35`, `#36`, `#69`; R2DBC modules `11-high-performance/04-06-*` | Covered by counterpart; `#50` is docs wiring |
| Spring Boot tenant strategy epic `#51`, implementations `#52`-`#55`, plus wiring `#56`; JDBC modules `10-multi-tenant/04-06-*`, `08-tenant-onboarding-spring-web` | R2DBC `#37`-`#42`; R2DBC modules `10-multi-tenant/03-06-*` | Covered by counterpart; `#56` is docs wiring |
| Chapter 12 production integration epic `#57` and split modules `12-production-integration/01-10-*` | R2DBC `#43`-`#49`; consolidated `12-production-integration/01-spring-production-integration`, `02-ktor-production-integration` | Covered by counterpart |
| R2DBC connection-factory-per-tenant | R2DBC `#39`; JDBC has database-per-tenant `#53` and schema-per-tenant `#52` instead | Platform-specific, no duplicate |
| JDBC DAO/entities examples | R2DBC has SQL DSL-only basic examples | Platform-specific; DAO is not a R2DBC teaching target |
| JDBC transaction template, Spring cache, benchmark | R2DBC uses suspend transactions, suspended cache, and routing examples | Platform-specific, no duplicate |

## Outcome

No new follow-up issues are required. All overlapping roadmap items are closed in both repositories, and the remaining
differences are caused by the blocking JDBC versus R2DBC API models.

The public parity table is mirrored in `README.md` and `README.ko.md` so future roadmap triage can link to a stable
document instead of reopening duplicate implementation issues.
