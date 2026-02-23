# RoutingDataSource 설계 가이드 (03-routing-datasource)

멀티테넌트/리드레플리카 환경에서 확장 가능한 `RoutingDataSource` 구조를 설명하는 설계 가이드 문서입니다. 정적 키 맵 방식의 한계를 줄이고, Registry 기반 동적 등록 패턴을 제안합니다.

## 학습 목표

- 정적 라우팅 구성의 운영 리스크를 이해한다.
- Registry 기반 동적 라우팅 아키텍처를 설계한다.
- 읽기/쓰기 분리와 테넌트 라우팅을 일관되게 통합한다.

## 선수 지식

- [`../10-multi-tenant/README.md`](../10-multi-tenant/README.md)

## 핵심 개념

- 역할 분리: `RoutingDataSource`는 키 결정만 담당
- 동적 등록: `DataSourceRegistry`에서 DataSource 관리
- 라우팅 키: tenant + readOnly 상태 조합

## 권장 아키텍처

| 구성 요소                           | 책임                        |
|---------------------------------|---------------------------|
| `DataSourceRegistry`            | 키 기반 DataSource 등록/조회     |
| `DynamicRoutingDataSource`      | 현재 라우팅 키로 DataSource 선택   |
| `ContextAwareRoutingDataSource` | tenant/readOnly로 라우팅 키 계산 |
| Auto Configuration              | 설정 기반 DataSource 자동 등록    |

## 적용 체크리스트

- 라우팅 키 오타/누락에 대한 예외 처리 정책을 명확히 한다.
- 신규 tenant 추가를 코드 수정 없이 설정만으로 처리한다.
- read-only 트랜잭션이 replica로 정확히 라우팅되는지 검증한다.

## 운영 체크포인트 (성능·안정성)

- 장애 노드 감지 시 라우팅 우회/복구 전략 필요
- Registry 갱신 시 동시성 안전성 보장
- 라우팅 결정 로그를 구조화해 추적 가능성 확보

## 다음 단계

- 본 가이드를 기반으로 실제 구현 모듈을 추가하고, 부하 테스트 지표(QPS, P95, 오류율)를 함께 수집합니다.
