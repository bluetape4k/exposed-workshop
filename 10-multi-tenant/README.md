# 10 Multi-Tenant (실전)

실전 멀티테넌트 아키텍처를 Exposed + Spring으로 구현하며 Schema 기반 테넌트 분리, 동적 라우팅, 컨텍스트 전파 흐름을 학습하는 챕터입니다.

## 챕터 목표

- 테넌트 식별/전파/격리 전체 흐름을 이해한다.
- Spring MVC, Virtual Thread, WebFlux 환경별 구현 차이를 비교한다.
- 운영 시 누수/격리 실패를 막는 검증 포인트를 확보한다.

## 선수 지식

- `09-spring` 내용
- 트랜잭션 및 DataSource 라우팅 기본 개념

## 포함 모듈

| 모듈                                       | 설명                            |
|------------------------------------------|-------------------------------|
| `01-multitenant-spring-web`              | Spring Web(MVC) 기반 멀티테넌트      |
| `02-multitenant-spring-web-virtualthread` | Virtual Thread 기반 멀티테넌트       |
| `03-multitenant-spring-webflux`          | WebFlux + Coroutines 기반 멀티테넌트 |

## 권장 학습 순서

1. `01-multitenant-spring-web`
2. `02-multitenant-spring-web-virtualthread`
3. `03-multitenant-spring-webflux`

## 실행 방법

```bash
# 개별 모듈 테스트
./gradlew :01-multitenant-spring-web:test
./gradlew :02-multitenant-spring-web-virtualthread:test
./gradlew :03-multitenant-spring-webflux:test
```

## 테스트 포인트

- `X-Tenant-Id` 누락/오입력 시 실패 동작을 검증한다.
- 테넌트 A 요청에서 테넌트 B 데이터가 노출되지 않는지 확인한다.
- 동시 요청 환경에서 컨텍스트 누수 여부를 검증한다.

## 성능·안정성 체크포인트

- 스키마 전환 비용과 커넥션 재사용 정책을 점검한다.
- ThreadLocal/Reactor Context 사용 시 컨텍스트 전파 누락을 방지한다.
- 운영 로그에 tenant 정보가 누락되지 않도록 추적성을 확보한다.

## 복잡한 시나리오

### 스키마 기반 테넌트 격리 + ThreadLocal 컨텍스트 전파 (Spring MVC)

`TenantFilter`가 `X-Tenant-Id` 헤더에서 테넌트를 추출해 `TenantContext`(ThreadLocal)에 저장하면, Exposed 트랜잭션 시작 전 `SchemaUtils.setSchema()`로 해당 스키마로 전환합니다. 동시 요청에서 테넌트 간 데이터 누수가 없는지 `AbstractMultitenantTest` 기반 통합 테스트로 검증합니다.

- 관련 모듈: [`01-multitenant-spring-web`](01-multitenant-spring-web/)
- 검증 테스트: [`01-multitenant-spring-web/src/test/kotlin/.../AbstractMultitenantTest.kt`](01-multitenant-spring-web/src/test/kotlin/exposed/examples/multitenant/springweb/AbstractMultitenantTest.kt)

### Virtual Thread 환경의 테넌트 컨텍스트 전파

Virtual Thread는 ThreadLocal이 아닌 `ScopedValue`로 컨텍스트를 전파합니다. `02-multitenant-spring-web-virtualthread`는 Virtual Thread 친화적인 테넌트 전파 방식을 보여주며, 기존 ThreadLocal 방식과의 차이를 비교할 수 있습니다.

- 관련 모듈: [`02-multitenant-spring-web-virtualthread`](02-multitenant-spring-web-virtualthread/)
- 검증 테스트: [`02-multitenant-spring-web-virtualthread/src/test/kotlin/.../AbstractMultitenantTest.kt`](02-multitenant-spring-web-virtualthread/src/test/kotlin/exposed/examples/multitenant/springweb/AbstractMultitenantTest.kt)

### WebFlux + Coroutines 환경의 Reactor Context 전파

WebFlux에서는 Reactor `Context`를 통해 코루틴 컨텍스트에 테넌트 정보를 전파합니다. `newSuspendedTransactionWithTenant`가 테넌트별 스키마 전환을 코루틴 안에서 안전하게 처리합니다.

- 관련 모듈: [`03-multitenant-spring-webflux`](03-multitenant-spring-webflux/)
- 검증 테스트: [`03-multitenant-spring-webflux/src/test/kotlin/.../AbstractMultitenantTest.kt`](03-multitenant-spring-webflux/src/test/kotlin/exposed/examples/multitenant/webflux/AbstractMultitenantTest.kt)

## 다음 챕터

- [11-high-performance](../11-high-performance/README.md): 고성능 캐시/라우팅 전략으로 확장합니다.
