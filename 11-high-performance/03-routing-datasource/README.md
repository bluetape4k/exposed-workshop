# RoutingDataSource 설계 가이드 (03-routing-datasource)

`03-routing-datasource`는 멀티테넌트 + 읽기/쓰기 분리 환경에서 확장 가능한
`RoutingDataSource`를 설계하기 위한 가이드 모듈입니다. 현재는 구현 코드 대신, 운영 친화적인 설계 원칙과 검증 전략을 문서로 제공합니다.

## 학습 목표

- 정적 라우팅(Map 고정) 방식의 운영 리스크를 이해한다.
- Registry 기반 동적 라우팅 아키텍처를 설계한다.
- 테넌트 라우팅과 read/write 분리를 일관된 규칙으로 통합한다.
- 장애 상황에서도 안정적으로 동작하는 예외/복구 정책을 정의한다.

## 선수 지식

- [`../../10-multi-tenant/README.md`](../../10-multi-tenant/README.md)
- [`../02-cache-strategies-coroutines/README.md`](../02-cache-strategies-coroutines/README.md)

## 설계 배경

정적 `targetDataSources` 맵은 초기 구성은 간단하지만, 다음 문제가 반복됩니다.

- 테넌트 추가 시 애플리케이션 재배포가 필요할 수 있다.
- 라우팅 키 오타/누락이 런타임에서 늦게 드러난다.
- read-only 트랜잭션 라우팅 검증이 약하면 primary 과부하가 발생한다.

따라서 `RoutingDataSource`는 "키 결정"에 집중하고,
`DataSourceRegistry`가 "데이터소스 생명주기(등록/조회/교체/제거)"를 담당하도록 분리합니다.

## 권장 아키텍처

| 구성 요소                           | 책임                               |
|---------------------------------|----------------------------------|
| `DataSourceRegistry`            | 라우팅 키 기반 DataSource 등록/조회/교체     |
| `DynamicRoutingDataSource`      | 현재 컨텍스트 키로 대상 DataSource 선택      |
| `ContextAwareRoutingDataSource` | `tenant + readOnly`로 최종 라우팅 키 계산 |
| Auto Configuration              | 설정 기반 DataSource 초기 등록 및 검증      |

## 라우팅 흐름

```mermaid
flowchart LR
    A["요청 수신"] --> B["TenantContext 확인"]
    B --> C["트랜잭션 readOnly 확인"]
    C --> D["RoutingKey 생성 (tenant:rw/ro)"]
    D --> E["DataSourceRegistry 조회"]
    E --> F["DynamicRoutingDataSource 선택"]
    F --> G["Exposed Transaction 실행"]
```

## 예제 구현 구성

README 설계를 기반으로 아래 예제 코드를 추가했습니다.

- `DataSourceRegistry`: 런타임 등록/조회 레이어
- `ContextAwareRoutingKeyResolver`: `tenant + readOnly` 키 계산
- `DynamicRoutingDataSource`: 계산된 키로 대상 DataSource 위임
- `RoutingDataSourceConfig`: `routing.datasource.*` 프로퍼티 기반 자동 등록
- `RoutingMarkerRepository`: Exposed DSL로 라우팅 결과 조회/적재

주요 파일:

- `src/main/kotlin/exposed/examples/routing/datasource/DataSourceRegistry.kt`
- `src/main/kotlin/exposed/examples/routing/datasource/InMemoryDataSourceRegistry.kt`
- `src/main/kotlin/exposed/examples/routing/datasource/ContextAwareRoutingKeyResolver.kt`
- `src/main/kotlin/exposed/examples/routing/datasource/DynamicRoutingDataSource.kt`
- `src/main/kotlin/exposed/examples/routing/config/RoutingDataSourceConfig.kt`
- `src/main/kotlin/exposed/examples/routing/domain/RoutingMarkerRepository.kt`
- `src/main/kotlin/exposed/examples/routing/web/RoutingMarkerController.kt`
- `src/test/kotlin/exposed/examples/routing/datasource/DynamicRoutingDataSourceTest.kt`
- `src/test/kotlin/exposed/examples/routing/web/RoutingMarkerControllerTest.kt`

## Endpoint 예제

- `GET /routing/marker`: read-write 라우팅 결과 조회
- `GET /routing/marker/readonly`: read-only 라우팅 결과 조회
- `PATCH /routing/marker`: 현재 tenant의 read-write 마커 갱신

tenant는 `X-Tenant-Id` 헤더로 전달합니다. (미전달 시 `default`)

실행:

```bash
./gradlew :03-routing-datasource:test
```

## 구현 시 권장 규칙

1. 키 규격 고정: `<tenant>:<rw|ro>` 형태를 표준으로 사용합니다.
2. 기본값 정책 명시: 컨텍스트 누락 시 `default:rw`로 갈지, 즉시 예외를 던질지 결정합니다.
3. 미등록 키 정책 명시: `IllegalStateException` 또는 `default` fallback 중 하나로 고정합니다.
4. Registry 동시성 보장: 등록/교체/조회는 lock-free 또는 적절한 동기화로 일관성을 확보합니다.

## 테스트 코드

| 파일                                                                                            | 설명                                      |
|-----------------------------------------------------------------------------------------------|------------------------------------------|
| [`ContextAwareRoutingKeyResolverTest.kt`](src/test/kotlin/exposed/examples/routing/datasource/ContextAwareRoutingKeyResolverTest.kt) | 테넌트·readOnly 조합별 라우팅 키 반환 검증 |
| [`DynamicRoutingDataSourceTest.kt`](src/test/kotlin/exposed/examples/routing/datasource/DynamicRoutingDataSourceTest.kt)             | 컨텍스트별 DataSource 라우팅 통합 검증    |
| [`InMemoryDataSourceRegistryTest.kt`](src/test/kotlin/exposed/examples/routing/datasource/InMemoryDataSourceRegistryTest.kt)         | 동시 등록·조회 스레드 안전성 검증         |
| [`RoutingMarkerControllerTest.kt`](src/test/kotlin/exposed/examples/routing/web/RoutingMarkerControllerTest.kt)                      | 테넌트별 REST API 라우팅 결과 검증        |

## 복잡한 시나리오

### 멀티테넌트 + 읽기/쓰기 분리 라우팅

`ContextAwareRoutingKeyResolver`는 `TenantContext`와 `TransactionSynchronizationManager.isCurrentTransactionReadOnly()`를 조합해 `<tenant>:<rw|ro>` 형태의 라우팅 키를 결정합니다. `DynamicRoutingDataSource`는 이 키로 `DataSourceRegistry`에서 실제 DataSource를 선택합니다.

- 관련 파일: [`ContextAwareRoutingKeyResolver.kt`](src/main/kotlin/exposed/examples/routing/datasource/ContextAwareRoutingKeyResolver.kt), [`DynamicRoutingDataSource.kt`](src/main/kotlin/exposed/examples/routing/datasource/DynamicRoutingDataSource.kt)
- 검증 테스트: [`ContextAwareRoutingKeyResolverTest.kt`](src/test/kotlin/exposed/examples/routing/datasource/ContextAwareRoutingKeyResolverTest.kt), [`DynamicRoutingDataSourceTest.kt`](src/test/kotlin/exposed/examples/routing/datasource/DynamicRoutingDataSourceTest.kt)

### Registry 동시성 안전성

`InMemoryDataSourceRegistry`는 `ConcurrentHashMap` 기반으로 구현돼 다수의 스레드가 동시에 DataSource를 등록/조회해도 레이스 컨디션 없이 동작합니다.

- 관련 파일: [`InMemoryDataSourceRegistry.kt`](src/main/kotlin/exposed/examples/routing/datasource/InMemoryDataSourceRegistry.kt)
- 검증 테스트: [`InMemoryDataSourceRegistryTest.kt`](src/test/kotlin/exposed/examples/routing/datasource/InMemoryDataSourceRegistryTest.kt)

## 테스트 전략

| 구분      | 검증 항목                      | 기대 결과                            |
|---------|----------------------------|----------------------------------|
| 라우팅 정합성 | `tenant=a, readOnly=true`  | `a:ro` DataSource 선택             |
| 라우팅 정합성 | `tenant=a, readOnly=false` | `a:rw` DataSource 선택             |
| 예외 처리   | 미등록 키 조회                   | 정의된 정책대로 예외 또는 fallback          |
| 동시성     | 등록/조회 병행                   | 레이스 없이 항상 유효한 DataSource 반환      |
| 장애 대응   | 특정 replica 실패              | primary 또는 대체 replica로 우회(정책 기반) |

## 운영 체크포인트 (성능·안정성)

- 장애 감지와 복구 시나리오를 분리합니다: "감지"와 "라우팅 우회"를 독립 컴포넌트로 둡니다.
- Registry 갱신 이벤트를 로깅/메트릭으로 남겨 변경 추적성을 확보합니다.
- 라우팅 로그에는 최소한 `tenant`, `readOnly`, `routingKey`, `selectedDataSource`를 포함합니다.

## 코드 리뷰 관점

- 성능: `determineCurrentLookupKey()` 경로에서 불필요한 객체 생성/문자열 연산을 줄입니다.
- 안정성: 컨텍스트 누락, 미등록 키, DataSource close 상태를 명시적으로 처리합니다.
- 테스트: 단순 정상 경로보다 "잘못된 키/장애/경합" 케이스를 우선 검증합니다.

## 다음 모듈

- 마지막 모듈입니다. 전체 고성능 예제 요약은 [`../README.md`](../README.md)에서 확인할 수 있습니다.
