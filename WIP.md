# WIP - exposed-workshop

스냅샷: 2026-06-02 KST
범위: 2026-01-01 이후 생성되고 `debop`에게 할당된 열린 GitHub 이슈.
열린 이슈 수: 1개.

## 최근 완료

- Spring Boot 4 정렬, version catalog 마이그레이션, 의존성 거버넌스,
  호환성 가드, Redisson 기준선 정렬이 병합되었습니다.
- 테스트 정리와 Kluent에서 `bluetape4k-assertions`로의 마이그레이션이 병합되었습니다.
- README hero/architecture 갱신이 병합되었습니다.
- GNO 기반 감사에서 routing datasource pool lifecycle 문제를 `#70`으로 등록했습니다.

## 현재 방향

새 chapter 콘텐츠를 늘리기 전에 기존 예제의 lifecycle 정확성을 먼저 회복합니다.
Chapter 10-12 backlog는 가치가 있지만, tenant 소유 datasource pool을 누수하는
예제 위에 더 확장해서는 안 됩니다.

## 우선순위 큐

| 우선순위 | 이슈 | 난이도 | 메모 |
|---|---|---:|---|
| P2 | [#70](https://github.com/bluetape4k/exposed-workshop/issues/70) routing datasource registry does not close tenant Hikari pools | M | 기존 chapter 11 예제가 tenant pool을 만들지만 registry/Spring shutdown 소유권을 정의하지 않습니다. |
| P3 | [#45](https://github.com/bluetape4k/exposed-workshop/issues/45) Ktor examples for chapters 10 and 11 epic | L | `#46`-`#50`의 parent입니다. 작업은 module 단위로 유지합니다. |
| P3 | [#46](https://github.com/bluetape4k/exposed-workshop/issues/46) Ktor multi-tenant example for chapter 10 | M | `#45`의 child입니다. |
| P3 | [#47](https://github.com/bluetape4k/exposed-workshop/issues/47) Ktor cache strategies example for chapter 11 | M | `#45`의 child입니다. |
| P3 | [#48](https://github.com/bluetape4k/exposed-workshop/issues/48) Ktor coroutine cache example for chapter 11 | M | `#45`의 child입니다. |
| P3 | [#49](https://github.com/bluetape4k/exposed-workshop/issues/49) Ktor routing datasource example for chapter 11 | M | `#45`의 child입니다. `#70` lifecycle fix를 반영해야 합니다. |
| P3 | [#50](https://github.com/bluetape4k/exposed-workshop/issues/50) wire Ktor chapter examples into docs and verification | S | `#46`-`#49` 완료 후 마무리합니다. |
| P3 | [#51](https://github.com/bluetape4k/exposed-workshop/issues/51) Spring Boot multi-tenant strategy epic | L | `#52`-`#56`의 parent입니다. |
| P3 | [#52](https://github.com/bluetape4k/exposed-workshop/issues/52) schema-per-tenant Spring Boot example | M | `#51`의 child입니다. |
| P3 | [#53](https://github.com/bluetape4k/exposed-workshop/issues/53) database-per-tenant Spring Boot example | M | `#51`의 child입니다. |
| P3 | [#54](https://github.com/bluetape4k/exposed-workshop/issues/54) Spring Security tenant authorization example | M | `#51`의 child입니다. |
| P3 | [#55](https://github.com/bluetape4k/exposed-workshop/issues/55) tenant onboarding/provisioning example | M | `#51`의 child입니다. |
| P3 | [#56](https://github.com/bluetape4k/exposed-workshop/issues/56) wire chapter 10 examples into docs and verification | S | `#52`-`#55` 완료 후 마무리합니다. |
| P3 | [#57](https://github.com/bluetape4k/exposed-workshop/issues/57) chapter 12 production integration epic | L | `#58`-`#63`의 parent입니다. |
| P3 | [#58](https://github.com/bluetape4k/exposed-workshop/issues/58) Spring Boot 4 and Ktor application architecture examples | M | `#57`의 child입니다. |
| P3 | [#59](https://github.com/bluetape4k/exposed-workshop/issues/59) authentication/session examples | M | `#57`의 child입니다. |
| P3 | [#60](https://github.com/bluetape4k/exposed-workshop/issues/60) outbox realtime examples | M | `#57`의 child입니다. |
| P3 | [#61](https://github.com/bluetape4k/exposed-workshop/issues/61) HTTP client outbox/idempotency examples | M | `#57`의 child입니다. |
| P3 | [#62](https://github.com/bluetape4k/exposed-workshop/issues/62) observability/readiness examples | M | `#57`의 child입니다. |
| P3 | [#63](https://github.com/bluetape4k/exposed-workshop/issues/63) wire chapter 12 examples into docs and verification | S | `#58`-`#62` 완료 후 마무리합니다. |

## 의존성 맵

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

## WIP 제한

| 작업 흐름 | 제한 | 현재 다음 작업 |
|---|---:|---|
| Correctness / lifecycle | 1 | routing datasource 확장보다 `#70`을 먼저 처리합니다. |
| Ktor examples | 1 | `#45` 아래 child 하나를 시작하고, children 완료 후 `#50`을 마무리합니다. |
| Spring Boot multi-tenant examples | 1 | `#51` 아래 child 하나를 시작하고, children 완료 후 `#56`을 마무리합니다. |
| Production integration examples | 1 | `#57` 아래 child 하나를 시작하고, children 완료 후 `#63`을 마무리합니다. |
