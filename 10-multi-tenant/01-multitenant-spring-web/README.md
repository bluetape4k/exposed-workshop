# Exposed + Spring Web + Multi-Tenant (01)

Spring MVC 환경에서 Exposed 기반 Schema 멀티테넌시를 구현하는 실전 예제입니다. 요청 헤더(
`X-Tenant-Id`)를 기준으로 테넌트 컨텍스트를 전파하고, 스키마를 분리해 데이터 격리를 보장합니다.

## 학습 목표

- 멀티테넌트 요청 흐름(식별 -> 컨텍스트 -> 스키마 선택)을 이해한다.
- ThreadLocal 기반 컨텍스트 관리 패턴을 익힌다.
- 테넌트 격리 실패를 테스트로 방지한다.

## 선수 지식

- [`../09-spring/README.md`](../09-spring/README.md)

## 핵심 개념

- `TenantFilter`: 요청에서 `X-Tenant-Id` 추출
- `TenantContext`: 현재 요청의 테넌트 저장/정리
- `TenantAwareDataSource` 및 스키마 라우팅

## 주요 구성 요소

| 파일/영역                             | 설명             |
|-----------------------------------|----------------|
| `tenant/TenantFilter.kt`          | 요청 단위 테넌트 식별   |
| `tenant/TenantContext.kt`         | 테넌트 컨텍스트 보관    |
| `tenant/TenantAwareDataSource.kt` | 테넌트 기반 라우팅     |
| `tenant/TenantSchemaAspect.kt`    | 스키마 적용 보조      |
| `controller/ActorController.kt`   | 테넌트 데이터 API 예제 |

## 실행 방법

```bash
./gradlew :exposed-10-multi-tenant-01-multitenant-spring-web:test
./gradlew :exposed-10-multi-tenant-01-multitenant-spring-web:bootRun
```

## API 실습 예시

```bash
curl -H 'X-Tenant-Id: tenant1' http://localhost:8080/actors
curl -H 'X-Tenant-Id: tenant2' http://localhost:8080/actors
```

## 실습 체크리스트

- 테넌트별 동일 API 호출 시 결과가 서로 분리되는지 확인
- 헤더 누락/잘못된 테넌트 값에 대한 실패 응답을 검증
- 요청 종료 후 컨텍스트가 반드시 정리되는지 확인

## 운영 체크포인트 (성능·안정성)

- ThreadLocal 누수 방지를 위해 필터 종료 시 clear 보장
- 로그/트레이스에 tenant 식별자 포함
- 신규 테넌트 온보딩 시 스키마 초기화 자동화 절차 확보

## 다음 모듈

- [`../02-mutitenant-spring-web-virtualthread/README.md`](../02-mutitenant-spring-web-virtualthread/README.md)
