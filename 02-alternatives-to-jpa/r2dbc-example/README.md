# 02 Alternatives: R2DBC Example

Spring Data R2DBC + Kotlin Coroutines를 이용해 비동기 데이터베이스 액세스를 구현하는 모듈입니다. Exposed보다 더 구체적으로 Reactive 리액터를 직접 활용하는 흐름을 보여줍니다.

## 학습 목표

- R2DBC `DatabaseClient`/`Repository`를 이용한 suspend CRUD를 이해한다.
- Coroutine 흐름에서 SQL 변수 바인딩/트랜잭션 허용성을 확인한다.
- Exposed와의 Latency/연결 모델 차이를 비교한다.

## 핵심 구성

- `MovieR2dbcRepository`: R2DBC `DatabaseClient` 기반 쿼리
- `MovieController`: suspend 함수 기반 REST API
- `R2dbcConfiguration`: H2/PostgreSQL URL, `ConnectionFactory` 설정

## 실행 방법

```bash
./gradlew :exposed-02-alternatives-to-jpa-r2dbc-example:bootRun
```

## 실습 체크리스트

- `Mono`/`Flux`가 아니라 suspend 함수로 결과를 검증
- 트랜잭션 속성(readOnly, timeout)을 변경하여 DB 반응 확인

## 성능·안정성 체크포인트

- R2DBC 커넥션 풀 크기 및 리스너 지연이 응답 시간에 미치는 영향 확인
- SQLInjection 방지를 위한 파라미터 바인딩 전략 주입 검증

## 다음 모듈

- [vertx-sqlclient-example](../vertx-sqlclient-example/README.md)
