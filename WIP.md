# WIP - exposed-workshop

스냅샷: 2026-08-29 KST
범위: 현재 `develop` 브랜치와 GitHub에서 `debop`에게 할당된 열린 이슈를
대조한 작업 큐.
열린 이슈 수: 1개.

## 최근 완료

`1.4.0` 예제 확장은 다음 이슈를 기준으로 현재 소스와 문서에 반영되어
있습니다.

- [#234](https://github.com/bluetape4k/exposed-workshop/issues/234) Apache
  Druid query-only Exposed 예제
- [#236](https://github.com/bluetape4k/exposed-workshop/issues/236)
  checkpointable JDBC batch 예제
- [#237](https://github.com/bluetape4k/exposed-workshop/issues/237) Ktor
  observability 예제의 Bluetape4k provider 전환
- [#238](https://github.com/bluetape4k/exposed-workshop/issues/238) Exposed
  measured 단위 컬럼 예제
- [#239](https://github.com/bluetape4k/exposed-workshop/issues/239) JaVers +
  Exposed 감사 이력 예제
- [#240](https://github.com/bluetape4k/exposed-workshop/issues/240) JDBC
  Lettuce repository cache 전략 예제
- [#252](https://github.com/bluetape4k/exposed-workshop/issues/252) JaVers
  삭제 lifecycle 감사 검증 보강

이로써 Chapter 10의 Ktor/onboarding 변형, Chapter 11의 Ktor/Lettuce 변형,
Chapter 12의 Spring/Ktor production integration 쌍, Chapter 13의 플랫폼·DDD·
감사 예제가 모두 소스 트리에 연결되어 있습니다.

## 현재 방향

현재 유일한 열린 작업은 [#255](https://github.com/bluetape4k/exposed-workshop/issues/255)
입니다. 기존 Chapter 10 예제를 공통 `TenantContext`의 reference consumer로
전환하되, 신규 모듈은 만들지 않습니다.

공개 snapshot 저장소에서 기본 좌표
`io.github.bluetape4k:bluetape4k-dependencies:2.0.0-SNAPSHOT`의 metadata/POM과
`bluetape4k-bom:2.0.0-SNAPSHOT`,
`io.github.bluetape4k.exposed:bluetape4k-exposed-bom:2.0.0-SNAPSHOT`,
`io.github.bluetape4k:bluetape4k-tenant:2.0.0-SNAPSHOT` 좌표를 확인했습니다.
tenant metadata는 `20260829072901`에 갱신되었고, 정상 Gradle
`dependencyInsight`는 timestamped tenant snapshot
`2.0.0-20260829.072901-1`을 local Maven override 없이 선택합니다.

upstream provider
[`bluetape4k-projects#1566`](https://github.com/bluetape4k/bluetape4k-projects/pull/1566)은
merge되었습니다. [`bluetape4k-projects#1565`](https://github.com/bluetape4k/bluetape4k-projects/issues/1565)와
[`bluetape4k-dependencies#213`](https://github.com/bluetape4k/bluetape4k-dependencies/issues/213)은
추적을 위해 열려 있지만, 현재 공개 artifact 해석을 막지는 않습니다. 사용자가
`2.0.0-SNAPSHOT` 기준으로 #255 구현을 진행하도록 결정한 뒤 추가한 versionless
`bluetape4k-tenant` alias와 두 기존 consumer 전환은 공개 snapshot 기준으로
재검증을 완료했습니다.

- `10-multi-tenant/06-spring-security-tenant-authorization-spring-web`
  — MVC/platform-thread `ThreadLocal` consumer
- `10-multi-tenant/02-multitenant-spring-web-virtualthread`
  — MVC/virtual-thread JDK 25 `ScopedValue` consumer

header parsing, Spring Security authorization, schema/database routing과 기존
route·격리 동작은 유지했습니다. 순차/병렬 격리, 중첩 scope 복원, 실패 후
cleanup을 대상 테스트로 확인했습니다.

현재 구현 증거:

- `02-multitenant-spring-web-virtualthread`: 공통 `ScopedValueTenantContext`와
  `TenantContexts` 경계로 전환, JDK 25 scope 테스트 포함
- `06-spring-security-tenant-authorization-spring-web`: 공통
  `ThreadLocalTenantContext`와 `TenantContexts` 경계로 전환, filter cleanup 및
  인증/격리 회귀 테스트 포함
- 공개 snapshot 정상 Gradle 해석 기준 대상 모듈 테스트: `02` 44개, `06` 32개 통과

## 우선순위 큐

| 상태 | 이슈 | 다음 조건 |
|---|---|---|
| 구현 완료 (Issue metadata 갱신 대기) | [#255](https://github.com/bluetape4k/exposed-workshop/issues/255) (`1.4.0`) MVC·virtual-thread 예제를 공통 `TenantContext` reference consumer로 전환 | Issue DoD/상태 갱신 후 종료 검토 |

## 의존성 맵

```text
bluetape4k-projects#1562
  -> bluetape4k-dependencies#213
    -> 기본 2.0.0-SNAPSHOT metadata/POM은 공개·확인됨
    -> tenant artifact·versionless alias 공개 및 POM/API 검증 (upstream #1566 merge)
      -> exposed-workshop#255 (implementation + normal resolution complete)
        -> chapter 10/06 MVC ThreadLocal consumer 전환 완료
        -> chapter 10/02 JDK 25 ScopedValue consumer 전환 완료
        -> 기존 route·인증·routing·격리 회귀 검증 완료
```

## WIP 제한

| 작업 흐름 | 동시 작업 수 | 현재 규칙 |
|---|---:|---|
| TenantContext reference consumer | 1 | `2.0.0-SNAPSHOT` catalog 기준 단일 순차 lane. 공개 tenant artifact의 정상 Gradle 해석과 대상 테스트를 재검증했으며, 후속 변경도 같은 검증 경계를 유지합니다. |
| 신규 예제 확장 | 0 | 1.4.0 예제 세트가 닫힌 상태이므로 #255의 lifecycle/compatibility 경계를 먼저 확인합니다. |
