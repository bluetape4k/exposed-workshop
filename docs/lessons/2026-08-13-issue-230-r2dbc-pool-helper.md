# Ktor Exposed R2DBC 풀 구성을 공통 helper 계약으로 고정한다

## 맥락

Ktor Exposed 예제는 `ConnectionFactoryOptions.parse`,
`ConnectionFactories.get`, `ConnectionPoolConfiguration.builder`를 직접
호출하고 있었다. 이 방식은 다른 Bluetape4k 예제와 설정 경계를 달리하고,
`bluetape4k-r2dbc:1.12.1`이 제공하는 URL/options 변환과 pool DSL을 재사용하지
못했다.

## 결정

`connectionFactoryOptionsOf(url)`와
`connectionPoolOf(options) { ... }`를 사용하도록 교체했다. 기존 H2 URL,
`maxSize = 2`, `initialSize = 1`은 유지하고, helper 기본 `minIdle = 8`이
작은 풀의 max size와 충돌하므로 `minIdle = 0`을 명시했다. 풀은
`KtorExposedIntegrationResources`가 소유하며 `R2dbcDatabase`에는 pool을
주입한다. `use`와 `ApplicationStopped`의 중복 close가 안전하도록 caller-owned
lifecycle 계약을 그대로 보존했다.

## 검증

RED 단계에서 테스트가 private pool 접근으로 컴파일되지 않는 것을 확인한
뒤, 테스트가 pool metrics의 `maxAllocatedSize = 2`, warmup 후
`allocatedSize = 1`, 미처분 상태를 검증하도록 구현했다. `close()`를 두 번
호출해도 예외가 없고 최종 `isDisposed = true`임을 확인했다.
`:05-ktor-exposed-integration:test`는 5개 테스트 모두
`BUILD SUCCESSFUL`이며, runtime classpath는
`io.github.bluetape4k:bluetape4k-r2dbc -> 1.12.1`을 선택한다.

## 예상 밖의 점

helper의 `minIdle` 기본값은 기존 builder의 암묵적 동작과 달랐다. 풀 크기
경계만 옮기면 시작 시 validation이 실패할 수 있으므로, 기본값을 확인하지
않은 단순 치환은 거부했다.

## 다음 방어선

R2DBC 풀 예제를 추가하거나 변경할 때는 URL/options 변환과 pool DSL을
우선 사용하고, `minIdle <= maxSize`, caller-owned lifecycle, warmup metrics,
중복 close를 회귀 테스트로 함께 고정한다.
