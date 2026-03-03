# 02 Alternatives: Vert.x SQL Client Example

Vert.x SQL Client + Kotlin Coroutines를 활용해 이벤트 기반/논블로킹 데이터베이스 작업을 구현한 모듈입니다. Reactive 스트림을 직접 제어하면서 Exposed/Reactive ORM과의 차이를 경험합니다.

## 학습 목표

- Vert.x SQL Client의 `SqlClient`/`SqlConnection`을 suspend로 감싼 패턴을 익힌다.
- 트랜잭션 경계를 Vert.x에서 직접 제어하는 패턴을 이해한다.
- 이벤트 루프 기반 API와 Exposed의 블로킹/Non-blocking 경로를 비교한다.

## 핵심 구성

- `MovieVertxRepository`: Vert.x SQL Client로 동작하는 Repository
- `MovieVertxHandler`: Vert.x Web 라우터 + suspend 핸들러
- `VertxConfiguration`: PostgreSQL/H2 커넥션 설정, 이벤트 루프 수 조정

## 실행 방법

```bash
./gradlew :vertx-sqlclient-example:bootRun
```

## 실습 체크리스트

- Vert.x Web 라우터에서 `RoutingContext`를 suspend 처리로 감쌌을 때 정상 응답 확인
- SQL Client의 파라미터/페이징 조합이 무결하게 수행되는지 검증

## 성능·안정성 체크포인트

- 이벤트 루프가 블로킹 호출로 대기하지 않도록 설계되었는지 확인
- 데이터베이스 커넥션/트랜잭션이 재사용되는지 모니터링

## 다음 챕터

- [03-exposed-basic](../03-exposed-basic/README.md)
