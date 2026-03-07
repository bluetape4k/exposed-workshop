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

## 다음 단계

- [`../README.md`](../README.md): 고성능 예제 전체 목록
- [`../../10-multi-tenant/README.md`](../../10-multi-tenant/README.md): 멀티테넌트 예제 흐름
