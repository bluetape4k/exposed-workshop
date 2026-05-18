# WIP - exposed-workshop

Snapshot: 2026-05-18 KST
Scope: open GitHub issues assigned to `debop`, created on or after 2026-01-01.
Open count: 20 issues.

## Recently Completed

- Spring Boot 4 alignment, version catalog migration, dependency governance,
  compatibility guards, and Redisson baseline alignment are merged.
- Test cleanup and Kluent to `bluetape4k-assertions` migration are merged.
- README hero/architecture refresh is merged.
- QMD-backed audit registered `#70` for routing datasource pool lifecycle.

## Current Direction

Restore lifecycle correctness in existing examples before adding more chapter
content. Chapter 10-12 backlog is valuable, but it should not expand on top of
examples that leak tenant-owned datasource pools.

## Priority Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P2 | [#70](https://github.com/bluetape4k/exposed-workshop/issues/70) routing datasource registry does not close tenant Hikari pools | M | Existing chapter 11 example creates tenant pools without registry/Spring shutdown ownership. |
| P3 | [#45](https://github.com/bluetape4k/exposed-workshop/issues/45) Ktor examples for chapters 10 and 11 epic | L | Parent for `#46`-`#50`; keep work module-scoped. |
| P3 | [#46](https://github.com/bluetape4k/exposed-workshop/issues/46) Ktor multi-tenant example for chapter 10 | M | Child of `#45`. |
| P3 | [#47](https://github.com/bluetape4k/exposed-workshop/issues/47) Ktor cache strategies example for chapter 11 | M | Child of `#45`. |
| P3 | [#48](https://github.com/bluetape4k/exposed-workshop/issues/48) Ktor coroutine cache example for chapter 11 | M | Child of `#45`. |
| P3 | [#49](https://github.com/bluetape4k/exposed-workshop/issues/49) Ktor routing datasource example for chapter 11 | M | Child of `#45`; should account for `#70` lifecycle fix. |
| P3 | [#50](https://github.com/bluetape4k/exposed-workshop/issues/50) wire Ktor chapter examples into docs and verification | S | Finish after `#46`-`#49`. |
| P3 | [#51](https://github.com/bluetape4k/exposed-workshop/issues/51) Spring Boot multi-tenant strategy epic | L | Parent for `#52`-`#56`. |
| P3 | [#52](https://github.com/bluetape4k/exposed-workshop/issues/52) schema-per-tenant Spring Boot example | M | Child of `#51`. |
| P3 | [#53](https://github.com/bluetape4k/exposed-workshop/issues/53) database-per-tenant Spring Boot example | M | Child of `#51`. |
| P3 | [#54](https://github.com/bluetape4k/exposed-workshop/issues/54) Spring Security tenant authorization example | M | Child of `#51`. |
| P3 | [#55](https://github.com/bluetape4k/exposed-workshop/issues/55) tenant onboarding/provisioning example | M | Child of `#51`. |
| P3 | [#56](https://github.com/bluetape4k/exposed-workshop/issues/56) wire chapter 10 examples into docs and verification | S | Finish after `#52`-`#55`. |
| P3 | [#57](https://github.com/bluetape4k/exposed-workshop/issues/57) chapter 12 production integration epic | L | Parent for `#58`-`#63`. |
| P3 | [#58](https://github.com/bluetape4k/exposed-workshop/issues/58) Spring Boot 4 and Ktor application architecture examples | M | Child of `#57`. |
| P3 | [#59](https://github.com/bluetape4k/exposed-workshop/issues/59) authentication/session examples | M | Child of `#57`. |
| P3 | [#60](https://github.com/bluetape4k/exposed-workshop/issues/60) outbox realtime examples | M | Child of `#57`. |
| P3 | [#61](https://github.com/bluetape4k/exposed-workshop/issues/61) HTTP client outbox/idempotency examples | M | Child of `#57`. |
| P3 | [#62](https://github.com/bluetape4k/exposed-workshop/issues/62) observability/readiness examples | M | Child of `#57`. |
| P3 | [#63](https://github.com/bluetape4k/exposed-workshop/issues/63) wire chapter 12 examples into docs and verification | S | Finish after `#58`-`#62`. |

## Dependency Map

```text
#70 routing datasource Hikari lifecycle
  -> #49 Ktor routing datasource example should inherit the cleanup rule
  -> any future tenant datasource examples should define shutdown ownership

#45 Ktor chapters 10/11 epic
  -> #46 Ktor multi-tenant
  -> #47 Ktor cache strategies
  -> #48 Ktor coroutine cache
  -> #49 Ktor routing datasource
  -> #50 docs and verification

#51 Spring Boot chapter 10 multi-tenant epic
  -> #52 schema-per-tenant
  -> #53 database-per-tenant
  -> #54 Spring Security tenant authorization
  -> #55 onboarding/provisioning
  -> #56 docs and verification

#57 chapter 12 production integration epic
  -> #58 application architecture
  -> #59 authentication/session
  -> #60 outbox realtime
  -> #61 HTTP client outbox/idempotency
  -> #62 observability/readiness
  -> #63 docs and verification
```

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| Correctness / lifecycle | 1 | `#70` before routing datasource expansion. |
| Ktor examples | 1 | Start one child under `#45`; finish `#50` after children. |
| Spring Boot multi-tenant examples | 1 | Start one child under `#51`; finish `#56` after children. |
| Production integration examples | 1 | Start one child under `#57`; finish `#63` after children. |
