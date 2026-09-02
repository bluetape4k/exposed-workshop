# WIP - exposed-workshop

스냅샷: 2026-09-02 KST
범위: 현재 `develop` 브랜치와 GitHub에서 `debop`에게 할당된 열린 이슈를
대조한 작업 큐.
열린 이슈 수: 2개.

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

현재 열린 작업은
[#259](https://github.com/bluetape4k/exposed-workshop/issues/259)와
[#260](https://github.com/bluetape4k/exposed-workshop/issues/260)입니다. #259는 안정
`bluetape4k-dependencies:2.0.0` catalog를 소비하도록 참조·검증·현재 문서를
정렬하고, #260은 그 검증에서 드러난 H2 provider 회귀를 모듈 범위에서 해결합니다.
신규 모듈은 만들지 않습니다. Chapter 10의 공통 `TenantContext` reference consumer
전환은 [#255](https://github.com/bluetape4k/exposed-workshop/issues/255)로 완료되었습니다.

Maven Central에서 기본 좌표
`io.github.bluetape4k:bluetape4k-dependencies:2.0.0`의 metadata/POM과
`bluetape4k-bom:2.0.0`, `io.github.bluetape4k.exposed:bluetape4k-exposed-bom:2.0.0`,
`io.github.bluetape4k:bluetape4k-tenant:2.0.0` 좌표를 확인했습니다. GitHub
Release [`2.0.0`](https://github.com/bluetape4k/bluetape4k-dependencies/releases/tag/2.0.0)는
2026-09-02에 공개되었고, 정상 Gradle `dependencyInsight`에서 안정 tenant
artifact를 local Maven override 없이 선택하는지 #259에서 재검증했습니다.
catalog와 현재 문서 정렬 및 governance 검사를 통과했습니다. 변경 전 `develop`와
동기화 branch 모두 `:11-checkpointable-batch:test`에서 H2 2.4.240의
`BATCH_JOB_EXEC_STATUS_ACTIVE_KEY_CHK` 오류가 재현되어 #260으로 등록했습니다.
전역 H2 버전은 2.4.240에 유지하고, checkpointable batch 모듈의 test runtime만
`h2-v2-check-workaround = 2.3.232`로 고정했습니다. 회귀 테스트는 수정 전 실패를
확인한 뒤 통과했으며, batch 9개와 Ktor 6개 대상 테스트도 통과했습니다. 최종
`clean build`는 `BUILD SUCCESSFUL in 13m 1s`와 `1102 actionable tasks`
(`1094 executed`, `6 from cache`, `2 up-to-date`)로 완료되었습니다.

upstream provider
[`bluetape4k-projects#1566`](https://github.com/bluetape4k/bluetape4k-projects/pull/1566)은
merge되었습니다. [`bluetape4k-projects#1565`](https://github.com/bluetape4k/bluetape4k-projects/issues/1565)와
[`bluetape4k-dependencies#213`](https://github.com/bluetape4k/bluetape4k-dependencies/issues/213)은
선행 작업으로 종료되었으며, 안정 release train은 #259의 downstream 동기화로
이어집니다. #255에서 추가한 versionless `bluetape4k-tenant` alias와 두 기존
consumer 전환은 안정 `2.0.0` artifact 기준으로 유지합니다.

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
- 공개 안정 `2.0.0` 정상 Gradle 해석 기준 대상 모듈 테스트: `02` 44개, `06` 32개 통과

## 우선순위 큐

| 상태 | 이슈 | 다음 조건 |
|---|---|---|
| 구현 완료 | [#255](https://github.com/bluetape4k/exposed-workshop/issues/255) MVC·virtual-thread 예제를 공통 `TenantContext` reference consumer로 전환 | 완료 상태와 안정 artifact 기준 유지 |
| 검증 완료·이슈 기록 유지 | [#259](https://github.com/bluetape4k/exposed-workshop/issues/259) `bluetape4k-dependencies:2.0.0` downstream 참조 갱신 | 안정 catalog, governance, targeted·전체 빌드 증거 보존 |
| 수정·검증 완료·이슈 기록 유지 | [#260](https://github.com/bluetape4k/exposed-workshop/issues/260) H2 2.4.240 CHECK cross-session 회귀 | 회귀·모듈 테스트·전체 빌드 증거 보존 및 upstream fix 후 workaround 재검토 |

## 의존성 맵

```text
bluetape4k-projects#1562
  -> bluetape4k-dependencies#213
    -> 안정 2.0.0 metadata/POM은 공개·확인됨
    -> tenant artifact·versionless alias 공개 및 POM/API 검증 (upstream #1566 merge)
      -> exposed-workshop#255 (implementation + normal resolution complete)
        -> exposed-workshop#259 (stable catalog handoff + full build)
          -> exposed-workshop#260 (H2 CHECK cross-session workaround + regression)
        -> chapter 10/06 MVC ThreadLocal consumer 전환 완료
        -> chapter 10/02 JDK 25 ScopedValue consumer 전환 완료
        -> 기존 route·인증·routing·격리 회귀 검증 완료
```

## WIP 제한

| 작업 흐름 | 동시 작업 수 | 현재 규칙 |
|---|---:|---|
| `bluetape4k-dependencies:2.0.0` reference sync + H2 follow-up | 1 | #259에서 안정 catalog와 versionless alias를 정렬한 뒤 #260의 모듈 범위 workaround를 검증합니다. 전역 H2 2.4.240과 중앙 BOM 위임을 유지하고 governance·전체 빌드로 확인합니다. |
| 신규 예제 확장 | 0 | 안정 release train downstream 정렬이 끝날 때까지 신규 모듈을 만들지 않습니다. |
