# 02 Alternatives: Hibernate Reactive Example

Hibernate Reactive + Mutiny + PostgreSQL 환경에서 선언적/Reactive 트랜잭션을 실습하는 모듈입니다. Exposed와 다른 Reactive ORM을 비교할 수 있도록 설계되었습니다.

## 학습 목표

- Hibernate Reactive의 `Mutiny.Session` 기반 비동기 CRUD를 이해한다.
- PostgreSQL에서 Reactive 흐름을 유지하며 이벤트를 처리한다.
- Exposed와의 API 대응을 비교하여 마이그레이션 전략을 도출한다.

## 핵심 구성

- `ReactiveMovieRepository`: Mutiny API로 비동기 조회/수정 구현
- `ActorResource`: Reactive REST API 핸들러
- `Application`: Reactive `Mutiny.SessionFactory` 및 PostgreSQL 연결 설정

## 실행 방법

```bash
./gradlew :hibernate-reactive-example:bootRun
```

## 테스트 포인트

- Reactive `Uni`/`Multi` 결과가 정상적으로 commit/rollback 되는지 확인
- PostgreSQL에서 수신한 데이터가 JSON 응답으로 정상 직렬화되는지 검증

## 성능·안정성 체크포인트

- Reactive 스트림이 backpressure 없이 High Throughput을 유지하는지 관측
- 예외 발생 시 `Uni.onFailure()`/`onItem()` 경로가 정상 동작하는지 확인

## 다음 모듈

- [r2dbc-example](../r2dbc-example/README.md)
