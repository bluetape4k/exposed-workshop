# 02 Alternatives: R2DBC Example

Spring Data R2DBC + Kotlin Coroutines를 이용해 비동기 데이터베이스 액세스를 구현하는 모듈입니다. Exposed보다 더 구체적으로 Reactive 리액터를 직접 활용하는 흐름을 보여줍니다.

## 학습 목표

- R2DBC `DatabaseClient`/`Repository`를 이용한 suspend CRUD를 이해한다.
- Coroutine 흐름에서 SQL 변수 바인딩/트랜잭션 허용성을 확인한다.
- Exposed와의 Latency/연결 모델 차이를 비교한다.

## 핵심 구성

- `PostRepository`: Spring Data R2DBC `CrudRepository` 기반 Post CRUD
- `CommentRepository`: Post ID 기반 댓글 조회 및 집계
- `CustomerRepository`: `DatabaseClient` 직접 활용 및 커스텀 `@Query` 예제
- `PostController`: suspend 함수 기반 REST API (`/posts`, `/posts/{id}`, `/posts/{id}/comments/count`)
- `R2dbcConfiguration`: H2/PostgreSQL URL, `ConnectionFactory` 설정

## 예제 구성

| 파일 | 설명 |
|------|------|
| `config/R2dbcConfigTest.kt` | R2DBC `ConnectionFactory` 빈 로딩 검증 |
| `domain/repository/PostRepositoryTest.kt` | Post CRUD 코루틴 테스트 |
| `domain/repository/CommentRespositoryTest.kt` | 댓글 조회·집계·삽입 테스트 |
| `domain/repository/CustomerRepositoryTest.kt` | `DatabaseClient` + 커스텀 쿼리 테스트 |
| `controller/PostControllerTest.kt` | WebTestClient 기반 REST API 통합 테스트 |

## 실행 방법

```bash
# 테스트 실행
./gradlew :02-alternatives-to-jpa:r2dbc-example:test

# 앱 실행 (H2 기본)
./gradlew :02-alternatives-to-jpa:r2dbc-example:bootRun
```

## Exposed vs R2DBC 비교

| 항목 | Exposed | Spring Data R2DBC |
|------|---------|-------------------|
| 쿼리 스타일 | 타입 안전 DSL / DAO Entity | Repository 인터페이스 / `DatabaseClient` |
| 트랜잭션 | `transaction { }` / `newSuspendedTransaction { }` | `@Transactional` / `TransactionalOperator` |
| 연결 모델 | JDBC (블로킹, Virtual Thread 활용) | R2DBC (완전 비동기 Non-blocking) |
| 학습 곡선 | Kotlin DSL 친화적 | Spring 생태계 친화적 |
| N+1 방지 | `.with()` eager loading | 수동 join 쿼리 필요 |

## 복잡한 시나리오

- **커스텀 쿼리**: [`CustomerRepositoryTest`](src/test/kotlin/alternative/r2dbc/example/domain/repository/CustomerRepositoryTest.kt) - `DatabaseClient`로 테이블 직접 생성 후 `findByFirstname` / `@Query` 어노테이션 검증
- **REST API 통합**: [`PostControllerTest`](src/test/kotlin/alternative/r2dbc/example/controller/PostControllerTest.kt) - `WebTestClient`로 HTTP 200/404 응답 및 댓글 수 집계 검증

## 실습 체크리스트

- `Mono`/`Flux`가 아니라 suspend 함수로 결과를 검증
- 트랜잭션 속성(readOnly, timeout)을 변경하여 DB 반응 확인

## 성능·안정성 체크포인트

- R2DBC 커넥션 풀 크기 및 리스너 지연이 응답 시간에 미치는 영향 확인
- SQLInjection 방지를 위한 파라미터 바인딩 전략 주입 검증

## 다음 모듈

- [vertx-sqlclient-example](../vertx-sqlclient-example/README.md)
