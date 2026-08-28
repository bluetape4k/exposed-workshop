# WIP - exposed-workshop

스냅샷: 2026-08-28 KST
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

먼저 upstream
[`bluetape4k-projects#1562`](https://github.com/bluetape4k/bluetape4k-projects/issues/1562)
와
[`bluetape4k-dependencies#213`](https://github.com/bluetape4k/bluetape4k-dependencies/issues/213)
의 공개 `2.0.0-SNAPSHOT` metadata/POM과 tenant artifact를 실제로 검증합니다.
검증 전에는 로컬 의존성 migration을 시작하지 않으며, 검증 후 다음 두 기존
consumer만 전환합니다.

- `10-multi-tenant/06-spring-security-tenant-authorization-spring-web`
  — MVC/platform-thread `ThreadLocal` consumer
- `10-multi-tenant/02-multitenant-spring-web-virtualthread`
  — MVC/virtual-thread JDK 25 `ScopedValue` consumer

header parsing, Spring Security authorization, schema/database routing과 기존
route·격리 동작은 유지하고, 순차/병렬 격리와 실패 후 cleanup을 확인합니다.

## 우선순위 큐

| 상태 | 이슈 | 다음 조건 |
|---|---|---|
| upstream 대기 | [#255](https://github.com/bluetape4k/exposed-workshop/issues/255) (`1.4.0`) MVC·virtual-thread 예제를 공통 `TenantContext` reference consumer로 전환 | 공개 snapshot metadata/POM 검증 → 두 기존 모듈 migration → 대상 테스트·detekt·`git diff --check` 통과 |

## 의존성 맵

```text
bluetape4k-projects#1562
  -> bluetape4k-dependencies#213
    -> 공개 2.0.0-SNAPSHOT tenant artifact metadata/POM 검증
      -> exposed-workshop#255
        -> chapter 10/06 MVC ThreadLocal consumer 전환
        -> chapter 10/02 JDK 25 ScopedValue consumer 전환
        -> 기존 route·인증·routing·격리 회귀 검증
```

## WIP 제한

| 작업 흐름 | 동시 작업 수 | 현재 규칙 |
|---|---:|---|
| TenantContext reference consumer | 1 | upstream snapshot의 공개 metadata/POM을 검증하기 전에는 두 대상 모듈의 dependency migration을 시작하지 않습니다. |
| 신규 예제 확장 | 0 | 1.4.0 예제 세트가 닫힌 상태이므로 #255의 lifecycle/compatibility 경계를 먼저 확인합니다. |
