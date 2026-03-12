# RoutingDataSource 예제 (03-routing-datasource)

`03-routing-datasource`는 멀티테넌트와 read/write 분리를 함께 다루는 동적 `DataSource`
라우팅 예제입니다. `tenant + transaction readOnly` 정보를 조합해
`<tenant>:<rw|ro>` 키를 만들고, 등록된 `DataSource`로 위임합니다.

## 학습 목표

- 정적 `targetDataSources` 맵 대신 Registry 기반 라우팅 구조를 구성한다.
- `TenantContext`와 `@Transactional(readOnly = true)`를 하나의 라우팅 규칙으로 통합한다.
- 미등록 키, 기본 tenant fallback, 동시 등록 같은 운영성 이슈를 테스트로 검증한다.

## 핵심 구성 요소

| 구성 요소 | 역할 |
|----------|------|
| `DataSourceRegistry` | 라우팅 키별 `DataSource` 등록/조회 |
| `ContextAwareRoutingKeyResolver` | 현재 tenant/readOnly 상태로 라우팅 키 계산 |
| `DynamicRoutingDataSource` | 계산된 키로 실제 `DataSource` 선택 |
| `RoutingDataSourceConfig` | `routing.datasource.*` 설정을 Hikari `DataSource`로 초기화 |
| `TenantHeaderFilter` | `X-Tenant-Id` 헤더를 `TenantContext`에 바인딩 |
| `RoutingMarkerRepository` | 선택된 데이터소스가 맞는지 마커 값으로 검증 |

## 라우팅 규칙

- 키 형식: `<tenant>:<rw|ro>`
- 기본 tenant: `routing.datasource.default-tenant`
- tenant 헤더: `X-Tenant-Id`
- read-only 요청: `@Transactional(readOnly = true)` 경로에서 `:ro` 선택
- `ro` 설정이 없으면 같은 tenant의 `rw` 설정을 재사용

## API 예제

- `GET /routing/marker`: 현재 read-write 라우팅 결과 조회
- `GET /routing/marker/readonly`: 현재 read-only 라우팅 결과 조회
- `PATCH /routing/marker`: 현재 tenant의 read-write 마커 갱신

예시:

```bash
curl -H 'X-Tenant-Id: acme' http://localhost:8080/routing/marker
curl -H 'X-Tenant-Id: acme' http://localhost:8080/routing/marker/readonly
curl -X PATCH -H 'X-Tenant-Id: acme' -H 'Content-Type: application/json' \
  -d '{"marker":"acme-rw-updated"}' \
  http://localhost:8080/routing/marker
```

## 테스트 범위

현재 모듈은 아래 시나리오를 포함해 검증합니다.

- `ContextAwareRoutingKeyResolverTest`: 기본 tenant, read-only, 공백 tenant fallback
- `DynamicRoutingDataSourceTest`: `rw/ro` 라우팅과 미등록 키 예외
- `InMemoryDataSourceRegistryTest`: 동시 등록 안정성
- `RoutingDataSourceConfigTest`: 설정 바인딩과 `ro` fallback
- `TenantHeaderFilterTest`: 헤더 바인딩과 공백 헤더 무시
- `RoutingMarkerControllerTest`: HTTP 경로별 실제 라우팅 결과

실행:

```bash
./gradlew :03-routing-datasource:test
./gradlew :03-routing-datasource:bootRun
```

## 관련 파일

- `src/main/kotlin/exposed/examples/routing/config/RoutingDataSourceConfig.kt`
- `src/main/kotlin/exposed/examples/routing/datasource/`
- `src/main/kotlin/exposed/examples/routing/web/`
- `src/test/kotlin/exposed/examples/routing/`

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
